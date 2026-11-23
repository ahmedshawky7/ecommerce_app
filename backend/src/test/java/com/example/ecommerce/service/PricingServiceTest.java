package com.example.ecommerce.service;

import com.example.ecommerce.dto.PricingBreakdownResponse;
import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @InjectMocks
    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pricingService, "taxRate", new BigDecimal("0.14"));
        ReflectionTestUtils.setField(pricingService, "shippingFee", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(pricingService, "freeShippingThreshold", new BigDecimal("100.00"));
    }

    @Test
    void shouldApplyPercentageCouponWithCapAndFreeShipping() {
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("VIP20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .maxDiscountAmount(new BigDecimal("30.00"))
                .minOrderAmount(new BigDecimal("50.00"))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .usageLimit(100)
                .usageCount(10)
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(couponRepository.findByCodeIgnoreCase("VIP20")).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.countByCouponIdAndUserId(1L, 7L)).thenReturn(0L);

        // Subtotal = 200 -> 20% = 40 -> capped at 30 -> discounted = 170 (>= 100 =>
        // free shipping)
        // Tax = 170 * 0.14 = 23.80 -> Total = 193.80
        PricingBreakdownResponse res = pricingService.calculatePreview(new BigDecimal("200.00"), "VIP20", 7L);

        assertEquals(new BigDecimal("30.00"), res.discountAmount());
        assertEquals(new BigDecimal("0.00"), res.shippingAmount());
        assertEquals(new BigDecimal("23.80"), res.taxAmount());
        assertEquals(new BigDecimal("193.80"), res.totalAmount());
        assertEquals("VIP20", res.appliedCouponCode());
    }

    @Test
    void shouldChargeShippingWhenBelowFreeThreshold() {
        // Subtotal = 50 -> shipping = 10 -> tax = 50 * 0.14 = 7.00 -> total = 67.00
        PricingBreakdownResponse res = pricingService.calculatePreview(new BigDecimal("50.00"), null, 7L);

        assertEquals(new BigDecimal("0.00"), res.discountAmount());
        assertEquals(new BigDecimal("10.00"), res.shippingAmount());
        assertEquals(new BigDecimal("7.00"), res.taxAmount());
        assertEquals(new BigDecimal("67.00"), res.totalAmount());
    }

    @Test
    void shouldRejectExpiredCoupon() {
        Coupon expired = Coupon.builder()
                .id(2L)
                .code("OLD10")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("10.00"))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(couponRepository.findByCodeIgnoreCase("OLD10")).thenReturn(Optional.of(expired));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculatePreview(new BigDecimal("100.00"), "OLD10", 1L));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    void shouldRejectCouponExceedingPerUserLimit() {
        Coupon coupon = Coupon.builder()
                .id(3L)
                .code("ONCE")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("15.00"))
                .expiresAt(LocalDateTime.now().plusDays(2))
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(couponRepository.findByCodeIgnoreCase("ONCE")).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.countByCouponIdAndUserId(3L, 99L)).thenReturn(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> pricingService.calculatePreview(new BigDecimal("100.00"), "ONCE", 99L));
    }

    @Test
    void shouldCapFixedDiscountAtSubtotalAndApplyZeroTaxOnFreeOrder() {
        Coupon coupon = Coupon.builder()
                .id(4L)
                .code("MEGA100")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("100.00"))
                .expiresAt(LocalDateTime.now().plusDays(3))
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(couponRepository.findByCodeIgnoreCase("MEGA100")).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.countByCouponIdAndUserId(4L, 1L)).thenReturn(0L);

        PricingBreakdownResponse res = pricingService.calculatePreview(new BigDecimal("40.00"), "MEGA100", 1L);

        assertEquals(new BigDecimal("40.00"), res.discountAmount());
        assertEquals(new BigDecimal("10.00"), res.shippingAmount());
        assertEquals(new BigDecimal("0.00"), res.taxAmount());
        assertEquals(new BigDecimal("10.00"), res.totalAmount());
    }

    @Test
    void shouldRejectCouponWhenBelowMinOrderAmountOrInactiveOrMaxUsageReached() {
        Coupon minOrderCoupon = Coupon.builder()
                .id(5L)
                .code("MIN200")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("20.00"))
                .minOrderAmount(new BigDecimal("200.00"))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(couponRepository.findByCodeIgnoreCase("MIN200")).thenReturn(Optional.of(minOrderCoupon));
        assertThrows(IllegalArgumentException.class,
                () -> pricingService.calculatePreview(new BigDecimal("150.00"), "MIN200", 1L));

        Coupon exhaustedCoupon = Coupon.builder()
                .id(6L)
                .code("LIMIT10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .usageLimit(10)
                .usageCount(10)
                .perUserLimit(1)
                .isActive(true)
                .build();

        when(couponRepository.findByCodeIgnoreCase("LIMIT10")).thenReturn(Optional.of(exhaustedCoupon));
        assertThrows(IllegalArgumentException.class,
                () -> pricingService.calculatePreview(new BigDecimal("150.00"), "LIMIT10", 1L));
    }
}