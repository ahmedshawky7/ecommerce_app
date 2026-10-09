package com.example.ecommerce.dto;

import com.example.ecommerce.entity.OrderStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    BigDecimal totalAmount,
    OrderStatus status,
    String shippingAddress,
    String phone,
    String paymentMethod,
    List<OrderItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {}