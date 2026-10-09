package com.example.ecommerce.dto;

import java.math.BigDecimal;

public record DashboardStatsResponse(
    BigDecimal totalRevenue,
    Long totalOrders,
    Long totalCustomers,
    Long totalProducts,
    Long pendingOrders,
    Long confirmedOrders,
    Long cancelledOrders
) {}