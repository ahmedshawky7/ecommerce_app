package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200)
    String name,

    @Size(max = 2000)
    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    Integer stockQuantity,

    String imageUrl,

    @NotNull(message = "Category ID is required")
    Long categoryId
) {}