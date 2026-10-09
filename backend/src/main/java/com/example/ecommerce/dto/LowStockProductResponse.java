package com.example.ecommerce.dto;

import java.math.BigDecimal;

public record LowStockProductResponse(
    Long productId,
    String productName,
    Integer stockQuantity,
    BigDecimal price
) {}