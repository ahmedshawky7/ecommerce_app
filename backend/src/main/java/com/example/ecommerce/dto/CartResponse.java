package com.example.ecommerce.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
    Long id,
    List<CartItemResponse> items,
    Integer totalItems,
    BigDecimal totalPrice
) implements Serializable {}