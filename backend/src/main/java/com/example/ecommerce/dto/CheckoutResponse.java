package com.example.ecommerce.dto;

import java.math.BigDecimal;

public record CheckoutResponse(
    Long orderId,
    String orderNumber,
    String clientSecret,
    BigDecimal totalAmount
) {}