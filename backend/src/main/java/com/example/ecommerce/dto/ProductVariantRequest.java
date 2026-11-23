package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductVariantRequest(
        Long id,

        @NotBlank(message = "SKU is required")
        @Size(max = 100)
        String sku,

        @Size(max = 50)
        String size,

        @Size(max = 50)
        String color,

        @NotNull(message = "Variant price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Variant stock quantity is required")
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stockQuantity,

        String imageUrl
) {}