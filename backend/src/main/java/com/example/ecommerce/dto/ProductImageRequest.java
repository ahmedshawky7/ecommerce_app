package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(
        @NotBlank(message = "Image URL is required")
        String imageUrl,

        @Size(max = 200)
        String altText,

        Integer displayOrder,

        boolean isPrimary
) {}