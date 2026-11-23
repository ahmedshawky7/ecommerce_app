package com.example.ecommerce.integration;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtService;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.NotificationProducer;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.ProductService;
import com.google.gson.Gson;
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ReviewAndCatalogIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @MockitoBean
    private NotificationProducer notificationProducer;

    private MockMvc mockMvc;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, ctx) -> LocalDateTime.parse(json.getAsString()))
            .create();

    private User admin;
    private User buyer;
    private User nonBuyer;
    private String adminToken;
    private String buyerToken;
    private String nonBuyerToken;
    private ProductResponse product;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        databaseCleaner.cleanAll();

        admin = userRepository.save(User.builder()
                .username("admin").email("admin@rev.com")
                .password(passwordEncoder.encode("pass123")).role(Role.ADMIN).build());

        buyer = userRepository.save(User.builder()
                .username("buyer").email("buyer@rev.com")
                .password(passwordEncoder.encode("pass123")).role(Role.CUSTOMER).build());

        nonBuyer = userRepository.save(User.builder()
                .username("nonbuyer").email("nonbuyer@rev.com")
                .password(passwordEncoder.encode("pass123")).role(Role.CUSTOMER).build());

        adminToken = jwtService.generateToken(admin);
        buyerToken = jwtService.generateToken(buyer);
        nonBuyerToken = jwtService.generateToken(nonBuyer);

        Category category = categoryRepository.save(Category.builder().name("Audio").build());

        product = productService.createProduct(new ProductRequest(
                "Studio Monitor",
                "Reference studio speakers",
                new BigDecimal("250.00"),
                10,
                null,
                category.getId(),
                List.of(new ProductVariantRequest(null, "MON-01", "Pair", "Black", new BigDecimal("250.00"), 10, null)),
                null), admin.getEmail());
    }

    @Test
    void onlyVerifiedDeliveredBuyersShouldReviewAndUpdateProductRating() throws Exception {
        // 1. Before purchase, buyer is not eligible
        mockMvc.perform(get("/api/products/{id}/reviews/eligibility", product.getId())
                .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false));

        ReviewRequest reviewReq = new ReviewRequest(5, "Top notch", "Crystal clear audio!");

        mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
                .header("Authorization", "Bearer " + buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(reviewReq)))
                .andExpect(status().isBadRequest());

        // 2. Buyer purchases and order becomes DELIVERED
        Long variantId = product.getVariants().get(0).getId();
        cartService.addToCart(buyer.getEmail(), new AddToCartRequest(product.getId(), variantId, 1));
        CheckoutResponse checkout = orderService.checkout(
                buyer.getEmail(),
                new CheckoutRequest("100 Studio Rd", "010000", "CASH_ON_DELIVERY", null),
                "rev-idem-1");
        orderService.updateStatus(checkout.orderId(), OrderStatus.PROCESSING);
        orderService.updateStatus(checkout.orderId(), OrderStatus.SHIPPED);
        orderService.updateStatus(checkout.orderId(), OrderStatus.DELIVERED);

        mockMvc.perform(get("/api/products/{id}/reviews/eligibility", product.getId())
                .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));

        // 3. Buyer submits review -> 200 OK and product rating updates
        String reviewBody = mockMvc.perform(post("/api/products/{id}/reviews", product.getId())
                .header("Authorization", "Bearer " + buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedPurchase").value(true))
                .andExpect(jsonPath("$.rating").value(5))
                .andReturn().getResponse().getContentAsString();

        ReviewResponse createdReview = gson.fromJson(reviewBody, ReviewResponse.class);
        assertEquals(new BigDecimal("5.00"),
                productRepository.findById(product.getId()).orElseThrow().getAverageRating());

        // 4. Non-buyer cannot edit or delete buyer's review (403 Forbidden)
        mockMvc.perform(put("/api/reviews/{reviewId}", createdReview.id())
                .header("Authorization", "Bearer " + nonBuyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(new ReviewRequest(1, "Spam", "Fake negative review"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reviews/{reviewId}", createdReview.id())
                .header("Authorization", "Bearer " + nonBuyerToken))
                .andExpect(status().isForbidden());

        // 5. Admin can delete review -> 204 No Content and rating resets
        mockMvc.perform(delete("/api/reviews/{reviewId}", createdReview.id())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertEquals(0, productRepository.findById(product.getId()).orElseThrow().getReviewCount());
    }

    @Test
    void catalogFilterEndpointShouldFilterByKeywordPriceAndStock() throws Exception {
        mockMvc.perform(get("/api/products/filter")
                .param("keyword", "studio")
                .param("minPrice", "200")
                .param("maxPrice", "300")
                .param("inStockOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Studio Monitor"));

        mockMvc.perform(get("/api/products/filter")
                .param("minPrice", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}