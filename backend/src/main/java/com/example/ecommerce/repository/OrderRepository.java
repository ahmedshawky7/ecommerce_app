package com.example.ecommerce.repository;

import com.example.ecommerce.dto.TopSellingProductResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

       @Query("SELECT o.id FROM Order o " +
                     "WHERE (:status IS NULL OR o.status = :status) " +
                     "AND (:from IS NULL OR o.createdAt >= :from) " +
                     "AND (:to IS NULL OR o.createdAt <= :to) " +
                     "AND (:keyword IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                     "ORDER BY o.createdAt DESC")
       Page<Long> adminSearchOrderIds(
                     @Param("status") OrderStatus status,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to,
                     @Param("keyword") String keyword,
                     Pageable pageable);

       @Query("SELECT o.id FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
       Page<Long> findOrderIdsByUserId(@Param("userId") Long userId, Pageable pageable);

       @Query("SELECT o.id FROM Order o ORDER BY o.createdAt DESC")
       Page<Long> findAllOrderIds(Pageable pageable);

       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "LEFT JOIN FETCH oi.variant " +
                     "LEFT JOIN FETCH o.user " +
                     "WHERE o.id IN :ids " +
                     "ORDER BY o.createdAt DESC")
       List<Order> findAllByIdInWithAllDetails(@Param("ids") List<Long> ids);

       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "LEFT JOIN FETCH oi.variant " +
                     "WHERE o.id IN :ids " +
                     "ORDER BY o.createdAt DESC")
       List<Order> findAllByIdInWithItems(@Param("ids") List<Long> ids);

       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "LEFT JOIN FETCH oi.variant " +
                     "LEFT JOIN FETCH o.user " +
                     "WHERE o.id = :id")
       Optional<Order> adminFindByIdWithAllDetails(@Param("id") Long id);

       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "LEFT JOIN FETCH oi.variant " +
                     "LEFT JOIN FETCH o.user " +
                     "WHERE o.id = :id")
       Optional<Order> findByIdWithItems(@Param("id") Long id);

       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "LEFT JOIN FETCH oi.variant " +
                     "LEFT JOIN FETCH o.user " +
                     "WHERE o.paymentIntentId = :paymentIntentId")
       Optional<Order> findByPaymentIntentIdWithItems(@Param("paymentIntentId") String paymentIntentId);

       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "LEFT JOIN FETCH oi.variant " +
                     "WHERE o.status = :status " +
                     "AND o.paymentMethod = 'STRIPE' " +
                     "AND o.expiresAt IS NOT NULL " +
                     "AND o.expiresAt < :now")
       List<Order> findExpiredPendingStripeOrders(
                     @Param("status") OrderStatus status,
                     @Param("now") LocalDateTime now);

       boolean existsByOrderNumber(String orderNumber);

       @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
                     "WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED')")
       BigDecimal getTotalRevenue();

       Long countByStatus(OrderStatus status);

       @Query("SELECT new com.example.ecommerce.dto.TopSellingProductResponse(" +
                     "p.id, p.name, p.imageUrl, SUM(oi.quantity), SUM(oi.subtotal)) " +
                     "FROM OrderItem oi " +
                     "JOIN oi.product p " +
                     "JOIN oi.order o " +
                     "WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED') " +
                     "GROUP BY p.id, p.name, p.imageUrl " +
                     "ORDER BY SUM(oi.quantity) DESC")
       List<TopSellingProductResponse> findTopSellingProducts(Pageable pageable);

       @Query(value = "SELECT DATE(o.created_at) as sale_date, COUNT(*) as orders_count, " +
                     "COALESCE(SUM(o.total_amount), 0) as revenue " +
                     "FROM orders o " +
                     "WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED') " +
                     "AND o.created_at >= :fromDate " +
                     "GROUP BY DATE(o.created_at) " +
                     "ORDER BY DATE(o.created_at) ASC", nativeQuery = true)
       List<Object[]> findDailySalesRaw(@Param("fromDate") LocalDateTime fromDate);

       @Query("SELECT c.id, c.name, SUM(oi.quantity), SUM(oi.subtotal) " +
                     "FROM OrderItem oi " +
                     "JOIN oi.product p " +
                     "JOIN p.category c " +
                     "JOIN oi.order o " +
                     "WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED') " +
                     "GROUP BY c.id, c.name " +
                     "ORDER BY SUM(oi.subtotal) DESC")
       List<Object[]> findRevenueByCategoryRaw();
}