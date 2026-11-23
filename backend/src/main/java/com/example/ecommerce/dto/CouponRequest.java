package com.example.ecommerce.dto;

import com.example.ecommerce.entity.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
        @NotBlank(message = "Coupon code is required")
        @Size(min = 3, max = 50)
        String code,

        @Size(max = 255)
        String description,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
        BigDecimal discountValue,

        @DecimalMin(value = "0.00")
        BigDecimal minOrderAmount,

        @DecimalMin(value = "0.01")
        BigDecimal maxDiscountAmount,

        LocalDateTime startsAt,

        @NotNull(message = "Expiration date is required")
        LocalDateTime expiresAt,

        @Min(value = 1)
        Integer usageLimit,

        @Min(value = 1)
        Integer perUserLimit,

        Boolean isActive
) {}