package com.example.ecommerce.service;

import com.example.ecommerce.dto.AdminOrderResponse;
import com.example.ecommerce.dto.DashboardStatsResponse;
import com.example.ecommerce.dto.LowStockProductResponse;
import com.example.ecommerce.dto.OrderItemResponse;
import com.example.ecommerce.dto.TopSellingProductResponse;
import com.example.ecommerce.dto.UserManagementResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ecommerce.dto.SalesAnalyticsResponse;
import com.example.ecommerce.dto.RevenueByCategoryResponse;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        BigDecimal totalRevenue = orderRepository.getTotalRevenue();

        return new DashboardStatsResponse(
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                orderRepository.count(),
                userRepository.count(),
                productRepository.count(),
                orderRepository.countByStatus(OrderStatus.PENDING),
                orderRepository.countByStatus(OrderStatus.CONFIRMED),
                orderRepository.countByStatus(OrderStatus.CANCELLED));
    }

    @Transactional(readOnly = true)
    public List<TopSellingProductResponse> getTopSellingProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return orderRepository.findTopSellingProducts(pageable);
    }

    @Transactional(readOnly = true)
    public List<LowStockProductResponse> getLowStockProducts(int threshold, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findLowStockProducts(threshold, pageable);
    }

    // ============ Order Management ============

    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> searchOrders(
            OrderStatus status, LocalDateTime from, LocalDateTime to,
            String keyword, Pageable pageable) {
        return orderRepository.adminSearchOrders(status, from, to, keyword, pageable)
                .map(this::toAdminOrderResponse);
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse getOrderById(Long id) {
        Order order = orderRepository.adminFindByIdWithAllDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toAdminOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getOrdersByUser(Long userId, Pageable pageable) {
        // تأكد إن المستخدم موجود
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        return orderRepository.adminFindOrdersByUser(userId, pageable)
                .map(this::toAdminOrderResponse);
    }

        // ============ User Management ============
    
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserManagementResponse> searchUsers(
            String keyword, com.example.ecommerce.entity.Role role, 
            org.springframework.data.domain.Pageable pageable) {
        return userRepository.adminSearchUsers(keyword, role, pageable)
                .map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserResponse(user);
    }

    @Transactional
    public UserManagementResponse toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // منع تعطيل نفسك (لو كنت Admin)
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        
        System.out.println("✅ User " + user.getEmail() + " status changed to: " + user.isEnabled());
        return toUserResponse(user);
    }

    private UserManagementResponse toUserResponse(User user) {
        return new UserManagementResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
        // ============ Sales Analytics ============

      // ============ Sales Analytics ============

     @Transactional(readOnly = true)
    public List<SalesAnalyticsResponse> getDailySales(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        
        return orderRepository.findDailySalesRaw(fromDate).stream()
                .map(row -> new SalesAnalyticsResponse(
                        toLocalDate(row[0]),  // ← تعديل: استخدام Method مساعدة
                        ((Number) row[1]).longValue(),
                        (java.math.BigDecimal) row[2]
                ))
                .toList();
    }

    // Method مساعدة للتحويل بين أنواع التاريخ المختلفة
    private LocalDate toLocalDate(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof LocalDate) return (LocalDate) dateObj;
        if (dateObj instanceof java.sql.Date) return ((java.sql.Date) dateObj).toLocalDate();
        if (dateObj instanceof java.util.Date) {
            return ((java.util.Date) dateObj).toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(dateObj.toString());
    }

    @Transactional(readOnly = true)
    public List<RevenueByCategoryResponse> getRevenueByCategory() {
        return orderRepository.findRevenueByCategoryRaw().stream()
                .map(row -> new RevenueByCategoryResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        (java.math.BigDecimal) row[3]
                ))
                .toList();
    }

    private AdminOrderResponse toAdminOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getProduct().getImageUrl(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()))
                .toList();

        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getPhone(),
                order.getPaymentMethod(),
                order.getPaymentIntentId(),
                order.getUser().getId(),
                order.getUser().getDisplayName(),
                order.getUser().getEmail(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}