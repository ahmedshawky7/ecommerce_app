package com.example.ecommerce.controller;

import com.example.ecommerce.dto.AdminOrderResponse;
import com.example.ecommerce.dto.DashboardStatsResponse;
import com.example.ecommerce.dto.LowStockProductResponse;
import com.example.ecommerce.dto.TopSellingProductResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.service.AdminService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.example.ecommerce.dto.UserManagementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import com.example.ecommerce.dto.SalesAnalyticsResponse;
import com.example.ecommerce.dto.RevenueByCategoryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin Dashboard APIs (requires ADMIN role)")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final AdminService adminService;

    // ============ Dashboard Stats ============
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ============ Top Selling Products ============
    @GetMapping("/dashboard/top-products")
    public ResponseEntity<List<TopSellingProductResponse>> getTopSellingProducts(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(adminService.getTopSellingProducts(limit));
    }

    // ============ Low Stock Products ============
    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<LowStockProductResponse>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(adminService.getLowStockProducts(threshold, limit));
    }

    // ============ Order Management ============

    @GetMapping("/orders")
    public ResponseEntity<Page<AdminOrderResponse>> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.searchOrders(status, from, to, keyword, pageable));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<AdminOrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getOrderById(id));
    }

    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<Page<AdminOrderResponse>> getOrdersByUser(
            @PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(adminService.getOrdersByUser(userId, pageable));
    }

    // ============ User Management ============

    @GetMapping("/users")
    public ResponseEntity<Page<UserManagementResponse>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.example.ecommerce.entity.Role role,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.searchUsers(keyword, role, pageable));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserManagementResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<UserManagementResponse> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserStatus(id));
    }

        // ============ Sales Analytics ============

    @GetMapping("/analytics/sales")
    public ResponseEntity<List<SalesAnalyticsResponse>> getDailySales(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(adminService.getDailySales(days));
    }

    @GetMapping("/analytics/revenue-by-category")
    public ResponseEntity<List<RevenueByCategoryResponse>> getRevenueByCategory() {
        return ResponseEntity.ok(adminService.getRevenueByCategory());
    }
}