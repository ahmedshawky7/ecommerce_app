package com.example.ecommerce.service;

import com.example.ecommerce.dto.CouponRequest;
import com.example.ecommerce.dto.CouponResponse;
import com.example.ecommerce.dto.PricingBreakdownResponse;
import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PricingBreakdownResponse validateCouponForUserCart(String userEmail, String code) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getVariant().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pricingService.calculatePreview(subtotal, code, user.getId());
    }

    @Transactional(readOnly = true)
    public PricingBreakdownResponse previewCartPricing(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BigDecimal subtotal = cartRepository.findByUserIdWithItems(user.getId())
                .map(cart -> cart.getItems().stream()
                        .map(item -> item.getVariant().getPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);

        return pricingService.calculatePreview(subtotal, null, user.getId());
    }

    @Transactional(readOnly = true)
    public Page<CouponResponse> getAllCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Coupon code already exists");
        }
        validateCouponRequest(request);

        Coupon coupon = Coupon.builder()
                .code(normalizedCode)
                .description(request.description())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(request.maxDiscountAmount())
                .startsAt(request.startsAt())
                .expiresAt(request.expiresAt())
                .usageLimit(request.usageLimit())
                .perUserLimit(request.perUserLimit() != null ? request.perUserLimit() : 1)
                .isActive(request.isActive() == null || request.isActive())
                .build();

        Coupon saved = couponRepository.save(coupon);
        auditService.log(
                "COUPON_CREATED",
                "COUPON",
                saved.getId(),
                null,
                saved.getCode() + " (" + saved.getDiscountType() + ": " + saved.getDiscountValue() + ")",
                "Created coupon " + saved.getCode()
        );
        return toResponse(saved);
    }

    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        String normalizedCode = request.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new IllegalArgumentException("Coupon code already exists");
        }
        validateCouponRequest(request);

        String oldVal = coupon.getCode() + " (" + coupon.getDiscountType() + ": " + coupon.getDiscountValue() + ")";

        coupon.setCode(normalizedCode);
        coupon.setDescription(request.description());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setStartsAt(request.startsAt());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setPerUserLimit(request.perUserLimit() != null ? request.perUserLimit() : 1);
        if (request.isActive() != null) {
            coupon.setActive(request.isActive());
        }

        Coupon updated = couponRepository.save(coupon);
        auditService.log(
                "COUPON_UPDATED",
                "COUPON",
                updated.getId(),
                oldVal,
                updated.getCode() + " (" + updated.getDiscountType() + ": " + updated.getDiscountValue() + ")",
                "Updated coupon " + updated.getCode()
        );
        return toResponse(updated);
    }

    @Transactional
    public CouponResponse toggleCouponStatus(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        boolean oldActive = coupon.isActive();
        coupon.setActive(!oldActive);
        Coupon saved = couponRepository.save(coupon);

        auditService.log(
                "COUPON_TOGGLED",
                "COUPON",
                saved.getId(),
                "active=" + oldActive,
                "active=" + saved.isActive(),
                "Toggled coupon " + saved.getCode()
        );
        return toResponse(saved);
    }

    private void validateCouponRequest(CouponRequest request) {
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }
        if (request.startsAt() != null && request.expiresAt().isBefore(request.startsAt())) {
            throw new IllegalArgumentException("Expiration date must be after start date");
        }
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                coupon.getUsageLimit(),
                coupon.getUsageCount(),
                coupon.getPerUserLimit(),
                coupon.isActive(),
                coupon.getCreatedAt()
        );
    }
}