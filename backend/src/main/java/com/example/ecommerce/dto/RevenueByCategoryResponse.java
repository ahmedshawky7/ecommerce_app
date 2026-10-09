package com.example.ecommerce.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record RevenueByCategoryResponse(
    Long categoryId,
    String categoryName,
    Long totalQuantitySold,
    BigDecimal totalRevenue
) implements Serializable {}