package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupon_usages", indexes = {
        @Index(name = "idx_coupon_usage_lookup", columnList = "coupon_id, user_id"),
        @Index(name = "idx_coupon_usage_order", columnList = "order_id")
})
@EqualsAndHashCode(of = "id")
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
    }
}