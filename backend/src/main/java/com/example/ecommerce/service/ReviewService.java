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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdWithUser(productId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean canUserReviewProduct(Long productId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return !reviewRepository.existsByUserIdAndProductId(user.getId(), productId)
                && reviewRepository.hasDeliveredPurchase(user.getId(), productId);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ReviewResponse createReview(Long productId, ReviewRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (!product.isActive()) {
            throw new IllegalArgumentException("Product not found");
        }

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new IllegalArgumentException("You have already reviewed this product");
        }

        if (!reviewRepository.hasDeliveredPurchase(user.getId(), productId)) {
            throw new IllegalArgumentException(
                    "Only customers with a delivered order for this product can leave a review");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.rating())
                .title(request.title())
                .comment(request.comment().trim())
                .verifiedPurchase(true)
                .build();

        Review saved = reviewRepository.save(review);
        reviewRepository.flush();
        recalculateProductRating(product);

        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request, String userEmail) {
        Review review = reviewRepository.findByIdWithUserAndProduct(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You can only edit your own review");
        }

        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment().trim());

        Review updated = reviewRepository.save(review);
        reviewRepository.flush();
        recalculateProductRating(review.getProduct());

        return toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteReview(Long reviewId, String userEmail) {
        Review review = reviewRepository.findByIdWithUserAndProduct(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isOwner = review.getUser().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to delete this review");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);
        reviewRepository.flush();
        recalculateProductRating(product);
    }

    private void recalculateProductRating(Product product) {
        Object[] raw = reviewRepository.getProductRatingSummary(product.getId());
        Object[] summary = (raw != null && raw.length > 0 && raw[0] instanceof Object[] nested)
                ? nested
                : raw;

        double avgDouble = (summary != null && summary.length > 0 && summary[0] instanceof Number n1)
                ? n1.doubleValue()
                : 0.0;
        int count = (summary != null && summary.length > 1 && summary[1] instanceof Number n2)
                ? n2.intValue()
                : 0;

        product.setAverageRating(BigDecimal.valueOf(avgDouble).setScale(2, RoundingMode.HALF_UP));
        product.setReviewCount(count);
        productRepository.save(product);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getDisplayName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.isVerifiedPurchase(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}