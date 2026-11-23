package com.example.ecommerce.integration;

import com.example.ecommerce.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestDatabaseCleaner {

    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ProcessedWebhookEventRepository processedWebhookEventRepository;
    @Autowired private IdempotencyRecordRepository idempotencyRecordRepository;
    @Autowired private CouponUsageRepository couponUsageRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    public void cleanAll() {
        auditLogRepository.deleteAll();
        processedWebhookEventRepository.deleteAll();
        idempotencyRecordRepository.deleteAll();
        couponUsageRepository.deleteAll();
        reviewRepository.deleteAll();
        wishlistRepository.deleteAll();
        addressRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        couponRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }
}