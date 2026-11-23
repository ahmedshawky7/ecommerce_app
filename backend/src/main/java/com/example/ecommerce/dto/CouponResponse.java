package com.example.ecommerce.dto;

import com.example.ecommerce.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        Integer usageLimit,
        Integer usageCount,
        Integer perUserLimit,
        boolean isActive,
        LocalDateTime createdAt
) {}