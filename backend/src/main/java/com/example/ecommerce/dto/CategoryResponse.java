package com.example.ecommerce.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CategoryResponse(
    Long id,
    String name,
    String description,
    String imageUrl,
    LocalDateTime createdAt,
    Long productCount
) implements Serializable {}