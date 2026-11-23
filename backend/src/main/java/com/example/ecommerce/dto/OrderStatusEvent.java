package com.example.ecommerce.dto;

import com.example.ecommerce.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderStatusEvent(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        String message,
        LocalDateTime timestamp
) {}