package com.example.ecommerce.controller;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ============ Customer Pricing & Coupon Validation ============

    @GetMapping("/api/coupons/pricing-preview")
    public ResponseEntity<PricingBreakdownResponse> previewPricing(Authentication authentication) {
        return ResponseEntity.ok(couponService.previewCartPricing(authentication.getName()));
    }

    @PostMapping("/api/coupons/validate")
    public ResponseEntity<PricingBreakdownResponse> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(couponService.validateCouponForUserCart(
                authentication.getName(), request.code()));
    }

    // ============ Admin Coupon Management ============

    @GetMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CouponResponse>> getAllCoupons(Pageable pageable) {
        return ResponseEntity.ok(couponService.getAllCoupons(pageable));
    }

    @PostMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @PutMapping("/api/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @PutMapping("/api/admin/coupons/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> toggleCouponStatus(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.toggleCouponStatus(id));
    }
}