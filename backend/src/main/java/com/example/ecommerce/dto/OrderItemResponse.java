package com.example.ecommerce.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        Long variantId,
        String sku,
        String variantLabel,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) implements Serializable {}