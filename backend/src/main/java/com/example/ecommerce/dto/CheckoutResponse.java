package com.example.ecommerce.dto;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long orderId,
        String orderNumber,
        String clientSecret,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String couponCode
) {}