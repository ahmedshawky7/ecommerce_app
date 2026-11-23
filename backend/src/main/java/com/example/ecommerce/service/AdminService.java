package com.example.ecommerce.service;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.RefreshTokenRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

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
        return orderRepository.findTopSellingProducts(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<LowStockProductResponse> getLowStockProducts(int threshold, int limit) {
        return productRepository.findLowStockProducts(threshold, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> searchOrders(
            OrderStatus status, LocalDateTime from, LocalDateTime to,
            String keyword, Pageable pageable) {
        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<Long> idPage = orderRepository.adminSearchOrderIds(status, from, to, normalizedKeyword, pageable);
        return fetchAdminOrdersPage(idPage, pageable);
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse getOrderById(Long id) {
        Order order = orderRepository.adminFindByIdWithAllDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toAdminOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getOrdersByUser(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        Page<Long> idPage = orderRepository.findOrderIdsByUserId(userId, pageable);
        return fetchAdminOrdersPage(idPage, pageable);
    }

    private Page<AdminOrderResponse> fetchAdminOrdersPage(Page<Long> idPage, Pageable pageable) {
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, Order> ordersById = orderRepository.findAllByIdInWithAllDetails(idPage.getContent()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        List<AdminOrderResponse> responses = idPage.getContent().stream()
                .map(ordersById::get)
                .filter(Objects::nonNull)
                .map(this::toAdminOrderResponse)
                .toList();

        return new PageImpl<>(responses, pageable, idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserManagementResponse> searchUsers(String keyword, Role role, Pageable pageable) {
        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return userRepository.adminSearchUsers(normalizedKeyword, role, pageable)
                .map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserResponse(user);
    }

    @Transactional
    public UserManagementResponse toggleUserStatus(Long id, String currentAdminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getEmail().equals(currentAdminEmail)) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }

        boolean oldStatus = user.isEnabled();
        boolean newStatus = !oldStatus;
        user.setEnabled(newStatus);
        userRepository.save(user);

        if (!newStatus) {
            refreshTokenRepository.revokeAllByUserId(user.getId());
        }

        auditService.log(
                "USER_STATUS_TOGGLED",
                "USER",
                user.getId(),
                "enabled=" + oldStatus,
                "enabled=" + newStatus,
                "Changed status for user: " + user.getEmail()
        );

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

    @Transactional(readOnly = true)
    public List<SalesAnalyticsResponse> getDailySales(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return orderRepository.findDailySalesRaw(fromDate).stream()
                .map(row -> new SalesAnalyticsResponse(
                        toLocalDate(row[0]),
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    private LocalDate toLocalDate(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof LocalDate ld) return ld;
        if (dateObj instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (dateObj instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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
                        (BigDecimal) row[3]
                ))
                .toList();
    }

    private AdminOrderResponse toAdminOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getVariant() != null ? item.getVariant().getId() : null,
                        item.getSku(),
                        item.getVariantLabel(),
                        item.getProductName(),
                        item.getProduct().getImageUrl(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()))
                .toList();

        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSubtotalAmount() != null ? order.getSubtotalAmount() : order.getTotalAmount(),
                order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO,
                order.getShippingAmount() != null ? order.getShippingAmount() : BigDecimal.ZERO,
                order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO,
                order.getCouponCode(),
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