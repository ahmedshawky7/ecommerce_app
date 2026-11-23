package com.example.ecommerce.service;

import com.example.ecommerce.dto.ReviewRequest;
import com.example.ecommerce.dto.ReviewResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User customer;
    private User otherUser;
    private User admin;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(10L)
                .username("buyer")
                .email("buyer@test.com")
                .password("encoded")
                .role(Role.CUSTOMER)
                .isEnabled(true)
                .build();

        otherUser = User.builder()
                .id(11L)
                .username("other")
                .email("other@test.com")
                .password("encoded")
                .role(Role.CUSTOMER)
                .isEnabled(true)
                .build();

        admin = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@test.com")
                .password("encoded")
                .role(Role.ADMIN)
                .isEnabled(true)
                .build();

        product = Product.builder()
                .id(100L)
                .name("Wireless Headphones")
                .price(new BigDecimal("99.00"))
                .stockQuantity(15)
                .isActive(true)
                .build();
    }

    @Test
    void shouldRejectReviewWhenUserHasNoDeliveredPurchase() {
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(customer.getId(), product.getId())).thenReturn(false);
        when(reviewRepository.hasDeliveredPurchase(customer.getId(), product.getId())).thenReturn(false);

        ReviewRequest req = new ReviewRequest(5, "Great!", "Loved the sound quality.");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(product.getId(), req, customer.getEmail())
        );

        assertTrue(ex.getMessage().contains("delivered order"));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateReviewBySameUser() {
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(customer.getId(), product.getId())).thenReturn(true);

        ReviewRequest req = new ReviewRequest(4, "Nice", "Second review attempt.");
        assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(product.getId(), req, customer.getEmail())
        );
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void shouldCreateVerifiedReviewAndRecalculateRating() {
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(reviewRepository.existsByUserIdAndProductId(customer.getId(), product.getId())).thenReturn(false);
        when(reviewRepository.hasDeliveredPurchase(customer.getId(), product.getId())).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(55L);
            return r;
        });
        when(reviewRepository.getProductRatingSummary(product.getId()))
                .thenReturn(new Object[]{4.5, 2L});

        ReviewResponse res = reviewService.createReview(
                product.getId(),
                new ReviewRequest(5, "Awesome", "Works flawlessly out of the box."),
                customer.getEmail()
        );

        assertEquals(55L, res.id());
        assertTrue(res.verifiedPurchase());
        assertEquals(new BigDecimal("4.50"), product.getAverageRating());
        assertEquals(2, product.getReviewCount());
        verify(productRepository).save(product);
    }

    @Test
    void shouldForbidUpdatingAnotherUsersReview() {
        Review existing = Review.builder()
                .id(77L)
                .product(product)
                .user(customer)
                .rating(5)
                .comment("Original comment")
                .build();

        when(reviewRepository.findByIdWithUserAndProduct(77L)).thenReturn(Optional.of(existing));

        assertThrows(
                AccessDeniedException.class,
                () -> reviewService.updateReview(
                        77L,
                        new ReviewRequest(1, "Hijack", "Trying to edit someone else review"),
                        otherUser.getEmail())
        );
    }

    @Test
    void shouldAllowAdminToDeleteAnyReviewAndResetRating() {
        Review existing = Review.builder()
                .id(77L)
                .product(product)
                .user(customer)
                .rating(5)
                .comment("Original comment")
                .build();

        when(reviewRepository.findByIdWithUserAndProduct(77L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(reviewRepository.getProductRatingSummary(product.getId()))
                .thenReturn(new Object[]{0.0, 0L});

        reviewService.deleteReview(77L, admin.getEmail());

        verify(reviewRepository).delete(existing);
        assertEquals(new BigDecimal("0.00"), product.getAverageRating());
        assertEquals(0, product.getReviewCount());
    }
}