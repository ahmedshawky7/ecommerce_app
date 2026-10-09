package com.example.ecommerce.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    String imageUrl,
    boolean isActive,
    Long categoryId,
    String categoryName,
    Long sellerId,
    String sellerName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {}