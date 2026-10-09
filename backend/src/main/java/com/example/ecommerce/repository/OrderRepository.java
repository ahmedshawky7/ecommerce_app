package com.example.ecommerce.repository;

import com.example.ecommerce.dto.RevenueByCategoryResponse;
import com.example.ecommerce.dto.SalesAnalyticsResponse;
import com.example.ecommerce.dto.TopSellingProductResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

       // ============ Admin Dashboard Queries ============
       // ============ Admin Order Management ============

       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "WHERE (:status IS NULL OR o.status = :status) " +
                     "AND (:from IS NULL OR o.createdAt >= :from) " +
                     "AND (:to IS NULL OR o.createdAt <= :to) " +
                     "AND (:keyword IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                     "ORDER BY o.createdAt DESC")
       Page<Order> adminSearchOrders(
                     @Param("status") OrderStatus status,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to,
                     @Param("keyword") String keyword,
                     Pageable pageable);

       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "WHERE o.user.id = :userId " +
                     "ORDER BY o.createdAt DESC")
       Page<Order> adminFindOrdersByUser(@Param("userId") Long userId, Pageable pageable);

       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "LEFT JOIN FETCH o.user " +
                     "WHERE o.id = :id")
       java.util.Optional<Order> adminFindByIdWithAllDetails(@Param("id") Long id);

       // إجمالي المبيعات (بحالة DELIVERED أو CONFIRMED)
       @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
                     "WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED')")
       java.math.BigDecimal getTotalRevenue();

       // عدد الطلبات حسب الحالة
       Long countByStatus(com.example.ecommerce.entity.OrderStatus status);

       // أفضل المنتجات مبيعاً
       @Query("SELECT new com.example.ecommerce.dto.TopSellingProductResponse(" +
                     "p.id, p.name, p.imageUrl, SUM(oi.quantity), SUM(oi.subtotal)) " +
                     "FROM OrderItem oi " +
                     "JOIN oi.product p " +
                     "JOIN oi.order o " +
                     "WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED') " +
                     "GROUP BY p.id, p.name, p.imageUrl " +
                     "ORDER BY SUM(oi.quantity) DESC")
       java.util.List<TopSellingProductResponse> findTopSellingProducts(Pageable pageable);

       @Query("SELECT DISTINCT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "WHERE o.user.id = :userId " +
                     "ORDER BY o.createdAt DESC")
       Page<Order> findByUserIdWithItems(@Param("userId") Long userId, Pageable pageable);

       @Query("SELECT o FROM Order o " +
                     "LEFT JOIN FETCH o.items oi " +
                     "LEFT JOIN FETCH oi.product " +
                     "WHERE o.id = :id")
       Optional<Order> findByIdWithItems(@Param("id") Long id);

       boolean existsByOrderNumber(String orderNumber);
       // ============ Sales Analytics ============

       @Query(value = "SELECT DATE(o.created_at) as sale_date, COUNT(*) as orders_count, " +
                     "COALESCE(SUM(o.total_amount), 0) as revenue " +
                     "FROM orders o " +
                     "WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED') " +
                     "AND o.created_at >= :fromDate " +
                     "GROUP BY DATE(o.created_at) " +
                     "ORDER BY DATE(o.created_at) ASC", nativeQuery = true)
       java.util.List<Object[]> findDailySalesRaw(@Param("fromDate") LocalDateTime fromDate);

       // الإيرادات حسب الفئة - استخدم Object[] كمان
       @Query("SELECT c.id, c.name, SUM(oi.quantity), SUM(oi.subtotal) " +
                     "FROM OrderItem oi " +
                     "JOIN oi.product p " +
                     "JOIN p.category c " +
                     "JOIN oi.order o " +
                     "WHERE o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED') " +
                     "GROUP BY c.id, c.name " +
                     "ORDER BY SUM(oi.subtotal) DESC")
       java.util.List<Object[]> findRevenueByCategoryRaw();
}