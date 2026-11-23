package com.example.ecommerce.repository;

import com.example.ecommerce.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    long countByCouponIdAndUserId(Long couponId, Long userId);

    Optional<CouponUsage> findByOrderId(Long orderId);
}