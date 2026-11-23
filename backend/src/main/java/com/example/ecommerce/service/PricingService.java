package com.example.ecommerce.service;

import com.example.ecommerce.dto.PricingBreakdownResponse;
import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Value("${app.pricing.tax-rate:0.14}")
    private BigDecimal taxRate;

    @Value("${app.pricing.shipping-fee:10.00}")
    private BigDecimal shippingFee;

    @Value("${app.pricing.free-shipping-threshold:100.00}")
    private BigDecimal freeShippingThreshold;

    @Transactional(readOnly = true)
    public PricingBreakdownResponse calculatePreview(BigDecimal subtotal, String couponCode, Long userId) {
        Coupon coupon = null;
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = couponRepository.findByCodeIgnoreCase(couponCode.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));
            validateCouponEligibility(coupon, subtotal, userId);
        }
        return buildBreakdown(subtotal, coupon);
    }

    public Coupon validateAndLockCoupon(String couponCode, BigDecimal subtotal, Long userId) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        Coupon coupon = couponRepository.findByCodeForUpdate(couponCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code"));
        validateCouponEligibility(coupon, subtotal, userId);
        return coupon;
    }

    public PricingBreakdownResponse buildBreakdown(BigDecimal subtotal, Coupon coupon) {
        BigDecimal safeSubtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String appliedCode = null;

        if (coupon != null && safeSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = safeSubtotal
                        .multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (coupon.getMaxDiscountAmount() != null
                        && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                    discountAmount = coupon.getMaxDiscountAmount().setScale(2, RoundingMode.HALF_UP);
                }
            } else {
                discountAmount = coupon.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
            }

            if (discountAmount.compareTo(safeSubtotal) > 0) {
                discountAmount = safeSubtotal;
            }
            appliedCode = coupon.getCode();
        }

        BigDecimal discountedSubtotal = safeSubtotal.subtract(discountAmount);

        BigDecimal shippingAmount;
        if (safeSubtotal.compareTo(BigDecimal.ZERO) == 0
                || discountedSubtotal.compareTo(freeShippingThreshold) >= 0) {
            shippingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            shippingAmount = shippingFee.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal taxAmount = discountedSubtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = discountedSubtotal.add(shippingAmount).add(taxAmount);

        return new PricingBreakdownResponse(
                safeSubtotal,
                discountAmount,
                shippingAmount,
                taxAmount,
                totalAmount,
                appliedCode
        );
    }

    private void validateCouponEligibility(Coupon coupon, BigDecimal subtotal, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        if (!coupon.isActive()) {
            throw new IllegalArgumentException("This coupon is no longer active");
        }
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
            throw new IllegalArgumentException("This coupon is not valid yet");
        }
        if (now.isAfter(coupon.getExpiresAt())) {
            throw new IllegalArgumentException("This coupon has expired");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new IllegalArgumentException("This coupon has reached its maximum usage limit");
        }
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order amount for this coupon is $" + coupon.getMinOrderAmount());
        }
        if (userId != null) {
            long userUsages = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
            if (userUsages >= coupon.getPerUserLimit()) {
                throw new IllegalArgumentException("You have already used this coupon the maximum number of times");
            }
        }
    }
}