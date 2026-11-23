package com.example.ecommerce.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String username,
        Integer rating,
        String title,
        String comment,
        boolean verifiedPurchase,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}