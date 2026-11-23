package com.example.ecommerce.service;

import com.example.ecommerce.dto.AdminAlertEvent;
import com.example.ecommerce.dto.OrderStatusEvent;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.Product;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RealtimeNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    public void notifyOrderUpdate(Order order, String customerMessage, String adminAlertType) {
        try {
            OrderStatusEvent customerEvent = new OrderStatusEvent(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    customerMessage,
                    LocalDateTime.now()
            );
            messagingTemplate.convertAndSendToUser(
                    order.getUser().getEmail(),
                    "/queue/orders",
                    customerEvent
            );

            if (adminAlertType != null) {
                AdminAlertEvent adminEvent = new AdminAlertEvent(
                        adminAlertType,
                        "Order " + order.getOrderNumber(),
                        "Status: " + order.getStatus() + " | Total: $" + order.getTotalAmount(),
                        order.getId(),
                        LocalDateTime.now()
                );
                messagingTemplate.convertAndSend("/topic/admin/alerts", adminEvent);
            }
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    public void notifyLowStockIfNeeded(Product product) {
        if (product.getStockQuantity() > lowStockThreshold) {
            return;
        }
        try {
            AdminAlertEvent alert = new AdminAlertEvent(
                    "LOW_STOCK",
                    "Low Stock Alert",
                    product.getName() + " has only " + product.getStockQuantity() + " items left",
                    product.getId(),
                    LocalDateTime.now()
            );
            messagingTemplate.convertAndSend("/topic/admin/alerts", alert);
        } catch (Exception e) {
            log.warn("Failed to push low-stock alert for product {}: {}", product.getId(), e.getMessage());
        }
    }
}