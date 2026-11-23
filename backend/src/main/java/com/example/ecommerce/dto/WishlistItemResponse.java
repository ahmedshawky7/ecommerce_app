package com.example.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long id,
        Long productId,
        String productName,
        String imageUrl,
        BigDecimal price,
        BigDecimal averageRating,
        Integer reviewCount,
        Integer stockQuantity,
        String categoryName,
        LocalDateTime addedAt
) {}