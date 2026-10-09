package com.example.ecommerce.dto;

import java.math.BigDecimal;

public record TopSellingProductResponse(
    Long productId,
    String productName,
    String productImageUrl,
    Long totalQuantitySold,
    BigDecimal totalRevenue
) {}