package com.example.ecommerce.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record CartItemResponse(
    Long id,
    Long productId,
    String productName,
    String productImageUrl,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal subtotal,
    Integer availableStock
) implements Serializable {}