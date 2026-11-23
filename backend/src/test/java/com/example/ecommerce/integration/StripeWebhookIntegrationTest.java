package com.example.ecommerce.integration;

import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import com.example.ecommerce.service.NotificationProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class StripeWebhookIntegrationTest {

    private static final String WEBHOOK_SECRET = "whsec_dummy";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository variantRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProcessedWebhookEventRepository processedWebhookEventRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @MockitoBean
    private NotificationProducer notificationProducer;

    private MockMvc mockMvc;
    private Order pendingOrder;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        databaseCleaner.cleanAll();

        User seller = userRepository.save(User.builder()
                .username("admin").email("admin@w.com").password(passwordEncoder.encode("p"))
                .role(Role.ADMIN).build());
        User buyer = userRepository.save(User.builder()
                .username("buyer").email("buyer@w.com").password(passwordEncoder.encode("p"))
                .role(Role.CUSTOMER).build());
        Category cat = categoryRepository.save(Category.builder().name("Cat").build());

        Product product = Product.builder()
                .name("Shoes").price(new BigDecimal("100.00")).stockQuantity(9)
                .category(cat).seller(seller).isActive(true).build();
        variant = ProductVariant.builder()
                .product(product).sku("SH-42").size("42").price(new BigDecimal("100.00"))
                .stockQuantity(9).isActive(true).build();
        product.getVariants().add(variant);
        product = productRepository.save(product);
        variant = product.getVariants().get(0);

        pendingOrder = Order.builder()
                .orderNumber("ORD-TEST0001")
                .user(buyer)
                .subtotalAmount(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("100.00"))
                .status(OrderStatus.PENDING)
                .shippingAddress("Cairo")
                .phone("0100")
                .paymentMethod("STRIPE")
                .paymentIntentId("cs_test_123")
                .build();
        pendingOrder.getItems().add(OrderItem.builder()
                .order(pendingOrder).product(product).variant(variant)
                .productName("Shoes").sku("SH-42").unitPrice(new BigDecimal("100.00"))
                .quantity(1).subtotal(new BigDecimal("100.00")).build());
        pendingOrder = orderRepository.save(pendingOrder);
    }

    @Test
    void shouldRejectInvalidSignature() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", "t=123,v1=bad")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldConfirmOrderOnceAndIgnoreDuplicateEvent() throws Exception {
        String payload = """
                {
                  "id": "evt_test_confirm_1",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_123",
                      "payment_status": "paid",
                      "amount_total": 10000,
                      "currency": "usd",
                      "metadata": { "orderId": "%d" }
                    }
                  }
                }
                """.formatted(pendingOrder.getId());

        String sig = signPayload(payload);

        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", sig)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(pendingOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, updated.getStatus());

        // Replay same event ID -> ignored cleanly
        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", signPayload(payload))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        verify(notificationProducer, times(1)).sendNotification(any());
    }

    @Test
    void shouldRestoreStockOnCheckoutSessionExpired() throws Exception {
        String payload = """
                {
                  "id": "evt_test_expire_1",
                  "type": "checkout.session.expired",
                  "data": {
                    "object": {
                      "id": "cs_test_123",
                      "metadata": { "orderId": "%d" }
                    }
                  }
                }
                """.formatted(pendingOrder.getId());

        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", signPayload(payload))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        assertEquals(OrderStatus.CANCELLED, orderRepository.findById(pendingOrder.getId()).orElseThrow().getStatus());
        assertEquals(10, variantRepository.findById(variant.getId()).orElseThrow().getStockQuantity());
    }

    private String signPayload(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signedContent = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
        String signature = HexFormat.of().formatHex(hash);
        return "t=" + timestamp + ",v1=" + signature;
    }
}
