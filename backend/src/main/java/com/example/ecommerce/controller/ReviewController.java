package com.example.ecommerce.controller;

import com.example.ecommerce.dto.ReviewRequest;
import com.example.ecommerce.dto.ReviewResponse;
import com.example.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, pageable));
    }

    @GetMapping("/api/products/{productId}/reviews/eligibility")
    public ResponseEntity<Map<String, Boolean>> checkEligibility(
            @PathVariable Long productId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("eligible", false));
        }
        boolean eligible = reviewService.canUserReviewProduct(productId, authentication.getName());
        return ResponseEntity.ok(Map.of("eligible", eligible));
    }

    @PostMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.createReview(productId, request, authentication.getName()));
    }

    @PutMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request, authentication.getName()));
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        reviewService.deleteReview(reviewId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}