package com.example.ecommerce.integration;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtService;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.NotificationProducer;
import com.example.ecommerce.service.ProductService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class CouponAndAdminRbacIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductService productService;
    @Autowired private CartService cartService;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TestDatabaseCleaner databaseCleaner;

    @MockitoBean
    private NotificationProducer notificationProducer;

    private MockMvc mockMvc;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .create();

    private String adminToken;
    private String customerToken;
    private User customer;
    private ProductResponse product;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        databaseCleaner.cleanAll();

        User admin = userRepository.save(User.builder()
                .username("admin").email("admin@cpn.com")
                .password(passwordEncoder.encode("pass123")).role(Role.ADMIN).build());
        customer = userRepository.save(User.builder()
                .username("cust").email("cust@cpn.com")
                .password(passwordEncoder.encode("pass123")).role(Role.CUSTOMER).build());

        adminToken = jwtService.generateToken(admin);
        customerToken = jwtService.generateToken(customer);

        Category cat = categoryRepository.save(Category.builder().name("Accessories").build());
        product = productService.createProduct(new ProductRequest(
                "USB-C Hub",
                "7-in-1 Adapter",
                new BigDecimal("100.00"),
                20,
                null,
                cat.getId(),
                List.of(new ProductVariantRequest(null, "HUB-7", "Std", "Gray", new BigDecimal("100.00"), 20, null)),
                null
        ), admin.getEmail());
    }

    @Test
    void customerShouldBeDeniedAdminCouponAndAuditEndpointsWhileAdminCanCreateAndCustomerCanValidate() throws Exception {
        CouponRequest couponReq = new CouponRequest(
                "SAVE25",
                "25% off",
                DiscountType.PERCENTAGE,
                new BigDecimal("25.00"),
                new BigDecimal("50.00"),
                new BigDecimal("30.00"),
                null,
                LocalDateTime.now().plusDays(10),
                100,
                1,
                true
        );

        // Customer blocked from Admin Coupon & Audit Log APIs
        mockMvc.perform(post("/api/admin/coupons")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(couponReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Admin creates coupon
        mockMvc.perform(post("/api/admin/coupons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(couponReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE25"));

        // Customer adds item ($100) -> validates coupon -> 25% off ($25), free shipping threshold ($100) not met on $75 => +$10 shipping + 14% tax ($10.50) = $95.50
        Long variantId = product.getVariants().get(0).getId();
        cartService.addToCart(customer.getEmail(), new AddToCartRequest(product.getId(), variantId, 1));

        mockMvc.perform(post("/api/coupons/validate")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(new ValidateCouponRequest("SAVE25"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(100.0))
                .andExpect(jsonPath("$.discountAmount").value(25.0))
                .andExpect(jsonPath("$.shippingAmount").value(10.0))
                .andExpect(jsonPath("$.taxAmount").value(10.5))
                .andExpect(jsonPath("$.totalAmount").value(95.5))
                .andExpect(jsonPath("$.appliedCouponCode").value("SAVE25"));

        // Admin can view audit logs
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}