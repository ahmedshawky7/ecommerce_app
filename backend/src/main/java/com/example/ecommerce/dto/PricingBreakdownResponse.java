package com.example.ecommerce.dto;

import java.math.BigDecimal;

public record PricingBreakdownResponse(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String appliedCouponCode
) {}