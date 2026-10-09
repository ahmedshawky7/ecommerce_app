package com.example.ecommerce.controller;

import com.example.ecommerce.dto.NotificationMessage;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.service.NotificationProducer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final OrderRepository orderRepository;
    private final NotificationProducer notificationProducer;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/stripe")
    @Transactional 
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // 1. التحقق من التوقيع
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("🔔 Webhook received! Event type: " + event.getType());

            // 2. التعامل مع حدث نجاح الدفع المباشر (Payment Intent)
            if ("payment_intent.succeeded".equals(event.getType())) {
                JsonObject rootObject = JsonParser.parseString(payload).getAsJsonObject();
                JsonObject paymentIntentNode = rootObject
                        .getAsJsonObject("data")
                        .getAsJsonObject("object");

                String paymentIntentId = paymentIntentNode.has("id") 
                        ? paymentIntentNode.get("id").getAsString() : "";

                String orderIdStr = "";
                if (paymentIntentNode.has("metadata")) {
                    JsonObject metadata = paymentIntentNode.getAsJsonObject("metadata");
                    if (metadata.has("orderId") && !metadata.get("orderId").isJsonNull()) {
                        orderIdStr = metadata.get("orderId").getAsString();
                    }
                }

                System.out.println("🔍 paymentIntentId: " + paymentIntentId);
                System.out.println("🔍 orderId from metadata (Payment Intent): " + orderIdStr);

                Order order = null;

                // محاولة 1: البحث بالـ orderId من الـ metadata
                if (!orderIdStr.isEmpty() && !orderIdStr.equals("null")) {
                    Long orderId = Long.valueOf(orderIdStr);
                    order = orderRepository.findById(orderId).orElse(null);
                }

                // محاولة 2: البحث بالـ paymentIntentId
                if (order == null && !paymentIntentId.isEmpty()) {
                    order = orderRepository.findAll().stream()
                            .filter(o -> paymentIntentId.equals(o.getPaymentIntentId()))
                            .findFirst()
                            .orElse(null);
                }

                // محاولة 3: البحث عن آخر Order بحالة PENDING (للاختبار)
                if (order == null) {
                    order = orderRepository.findAll().stream()
                            .filter(o -> o.getStatus() == OrderStatus.PENDING)
                            .reduce((first, second) -> second)
                            .orElse(null);
                }

                if (order != null && order.getStatus() == OrderStatus.PENDING) {
                    confirmOrder(order);
                } else {
                    System.out.println("⚠️ No PENDING order found to confirm for Payment Intent");
                }
            }

            // 3. التعامل مع حدث اكتمال جلسة الدفع (Checkout Session)
            else if ("checkout.session.completed".equals(event.getType())) {
                JsonObject rootObject = JsonParser.parseString(payload).getAsJsonObject();
                JsonObject sessionNode = rootObject
                        .getAsJsonObject("data")
                        .getAsJsonObject("object");

                String sessionId = sessionNode.has("id") ? sessionNode.get("id").getAsString() : "";
                String orderIdStr = "";
                if (sessionNode.has("metadata")) {
                    JsonObject metadata = sessionNode.getAsJsonObject("metadata");
                    if (metadata.has("orderId") && !metadata.get("orderId").isJsonNull()) {
                        orderIdStr = metadata.get("orderId").getAsString();
                    }
                }

                System.out.println("🔔 Checkout session completed: " + sessionId);
                System.out.println("🔍 orderId from metadata (Checkout): " + orderIdStr);

                if (!orderIdStr.isEmpty()) {
                    Long orderId = Long.valueOf(orderIdStr);
                    Order order = orderRepository.findById(orderId).orElse(null);
                    if (order != null && order.getStatus() == OrderStatus.PENDING) {
                        confirmOrder(order);
                        System.out.println("✅ Order confirmed via Checkout: " + order.getOrderNumber());
                    } else {
                        System.out.println("⚠️ No PENDING order found to confirm for Checkout Session");
                    }
                }
            }

            return ResponseEntity.ok("Webhook received");
        } catch (SignatureVerificationException e) {
            System.err.println("❌ Invalid signature: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            System.err.println("❌ Webhook error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }

    /**
     * ميثود مساعدة لتحديث حالة الطلب وإرسال الإشعار لتجنب تكرار الكود
     */
    private void confirmOrder(Order order) {
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        System.out.println("✅ Order confirmed: " + order.getOrderNumber());

        NotificationMessage notification = new NotificationMessage(
                order.getUser().getEmail(),
                "Order Confirmed - " + order.getOrderNumber(),
                "Great news! Your payment was successful.\n\n" +
                        "Order Number: " + order.getOrderNumber() + "\n" +
                        "Total Amount: $" + order.getTotalAmount() + "\n" +
                        "We will notify you once your order is shipped.");
        notificationProducer.sendNotification(notification);
    }
}
