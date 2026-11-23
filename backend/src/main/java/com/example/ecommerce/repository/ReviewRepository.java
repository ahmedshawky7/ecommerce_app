package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.user WHERE r.product.id = :productId ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Page<Review> findByProductIdWithUser(@Param("productId") Long productId, Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.product WHERE r.id = :id")
    Optional<Review> findByIdWithUserAndProduct(@Param("id") Long id);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.user.id = :userId " +
           "AND oi.product.id = :productId " +
           "AND o.status = com.example.ecommerce.entity.OrderStatus.DELIVERED")
    boolean hasDeliveredPurchase(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT COALESCE(AVG(r.rating), 0), COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Object[] getProductRatingSummary(@Param("productId") Long productId);
}