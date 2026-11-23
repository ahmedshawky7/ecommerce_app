package com.example.ecommerce.integration;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.CouponService;
import com.example.ecommerce.service.NotificationProducer;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyAndIdempotencyTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CouponService couponService;
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
    private CartRepository cartRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private CouponUsageRepository couponUsageRepository;
    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @MockitoBean
    private NotificationProducer notificationProducer;

    private Category category;
    private User admin;
    private User buyer1;
    private User buyer2;

    @BeforeEach
    void setUp() {
        databaseCleaner.cleanAll();

        admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@shop.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.ADMIN)
                .build());

        buyer1 = userRepository.save(User.builder()
                .username("buyer1")
                .email("buyer1@shop.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.CUSTOMER)
                .build());

        buyer2 = userRepository.save(User.builder()
                .username("buyer2")
                .email("buyer2@shop.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.CUSTOMER)
                .build());

        category = categoryRepository.save(Category.builder().name("Electronics").build());
    }

    @Test
    void checkoutWithSameIdempotencyKeyShouldNotDuplicateOrderOrStockDeduction() {
        ProductResponse product = productService.createProduct(new ProductRequest(
                "Mechanical Keyboard",
                "RGB",
                new BigDecimal("100.00"),
                10,
                null,
                category.getId(),
                List.of(new ProductVariantRequest(null, "KB-BLK", "TKL", "Black", new BigDecimal("100.00"), 10, null)),
                null), admin.getEmail());

        Long variantId = product.getVariants().get(0).getId();

        couponService.createCoupon(new CouponRequest(
                "SAVE10", "10% off", DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                BigDecimal.ZERO, null, null, LocalDateTime.now().plusDays(5), 50, 1, true));

        cartService.addToCart(buyer1.getEmail(), new AddToCartRequest(product.getId(), variantId, 2));

        CheckoutRequest req = new CheckoutRequest("123 Main St", "0100000000", "CASH_ON_DELIVERY", "SAVE10");
        String idemKey = "idem-key-12345";

        CheckoutResponse first = orderService.checkout(buyer1.getEmail(), req, idemKey);
        CheckoutResponse second = orderService.checkout(buyer1.getEmail(), req, idemKey);

        assertEquals(first.orderId(), second.orderId());
        assertEquals(first.orderNumber(), second.orderNumber());
        assertEquals(1, orderRepository.count());

        ProductVariant updatedVariant = variantRepository.findById(variantId).orElseThrow();
        assertEquals(8, updatedVariant.getStockQuantity());

        Coupon updatedCoupon = couponRepository.findByCodeIgnoreCase("SAVE10").orElseThrow();
        assertEquals(1, updatedCoupon.getUsageCount());
    }

    @Test
    void concurrentCheckoutsForLastStockItemShouldAllowOnlyOneWinner() throws Exception {
        ProductResponse product = productService.createProduct(new ProductRequest(
                "Limited GPU",
                "1 left",
                new BigDecimal("500.00"),
                1,
                null,
                category.getId(),
                List.of(new ProductVariantRequest(null, "GPU-1", "Standard", "Silver", new BigDecimal("500.00"), 1,
                        null)),
                null), admin.getEmail());

        Long variantId = product.getVariants().get(0).getId();

        cartService.addToCart(buyer1.getEmail(), new AddToCartRequest(product.getId(), variantId, 1));
        cartService.addToCart(buyer2.getEmail(), new AddToCartRequest(product.getId(), variantId, 1));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> task1 = () -> {
            ready.countDown();
            start.await();
            try {
                orderService.checkout(buyer1.getEmail(),
                        new CheckoutRequest("Addr 1", "111", "CASH_ON_DELIVERY", null), "k1");
                return true;
            } catch (Exception e) {
                return false;
            }
        };

        Callable<Boolean> task2 = () -> {
            ready.countDown();
            start.await();
            try {
                orderService.checkout(buyer2.getEmail(),
                        new CheckoutRequest("Addr 2", "222", "CASH_ON_DELIVERY", null), "k2");
                return true;
            } catch (Exception e) {
                return false;
            }
        };

        Future<Boolean> f1 = pool.submit(task1);
        Future<Boolean> f2 = pool.submit(task2);
        ready.await();
        start.countDown();

        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(r1 ^ r2, "Exactly one concurrent checkout must succeed");
        assertEquals(1, orderRepository.count());
        assertEquals(0, variantRepository.findById(variantId).orElseThrow().getStockQuantity());
    }

    @Test
    void cancellingConfirmedOrderShouldRestoreStockCouponAndWriteAuditLog() {
        ProductResponse product = productService.createProduct(new ProductRequest(
                "Smart Watch",
                "OLED",
                new BigDecimal("120.00"),
                5,
                null,
                category.getId(),
                List.of(new ProductVariantRequest(null, "SW-44", "44mm", "Black", new BigDecimal("120.00"), 5, null)),
                null), admin.getEmail());
        Long variantId = product.getVariants().get(0).getId();

        couponService.createCoupon(new CouponRequest(
                "WATCH20", "Flat 20", DiscountType.FIXED_AMOUNT, new BigDecimal("20.00"),
                BigDecimal.ZERO, null, null, LocalDateTime.now().plusDays(5), 10, 1, true));

        cartService.addToCart(buyer1.getEmail(), new AddToCartRequest(product.getId(), variantId, 2));
        CheckoutResponse checkout = orderService.checkout(
                buyer1.getEmail(),
                new CheckoutRequest("Cairo", "010123", "CASH_ON_DELIVERY", "WATCH20"),
                "k-cancel");

        assertEquals(3, variantRepository.findById(variantId).orElseThrow().getStockQuantity());

        orderService.updateStatus(checkout.orderId(), OrderStatus.CANCELLED);

        assertEquals(5, variantRepository.findById(variantId).orElseThrow().getStockQuantity());
        assertEquals(0, couponRepository.findByCodeIgnoreCase("WATCH20").orElseThrow().getUsageCount());
        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(a -> "ORDER_STATUS_UPDATED".equals(a.getAction()) && "CANCELLED".equals(a.getNewValue())));
    }
}
