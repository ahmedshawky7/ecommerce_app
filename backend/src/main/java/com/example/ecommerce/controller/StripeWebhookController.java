package com.example.ecommerce.controller;

import com.example.ecommerce.dto.NotificationMessage;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.ProcessedWebhookEvent;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProcessedWebhookEventRepository;
import com.example.ecommerce.service.AuditService;
import com.example.ecommerce.service.NotificationProducer;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.RealtimeNotificationService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final OrderRepository orderRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final NotificationProducer notificationProducer;
    private final RealtimeNotificationService realtimeNotificationService;
    private final AuditService auditService;
    private final OrderService orderService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${app.stripe.currency:usd}")
    private String expectedCurrency;

    @PostMapping("/stripe")
    @Transactional
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        if (sigHeader == null || sigHeader.isBlank()) {
            return ResponseEntity.badRequest().body("Missing Stripe-Signature header");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.warn("Malformed Stripe webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Malformed payload");
        }

        if (processedWebhookEventRepository.existsByEventId(event.getId())) {
            log.info("Duplicate Stripe event ignored: {}", event.getId());
            return ResponseEntity.ok("Event already processed");
        }

        JsonObject rootObject = JsonParser.parseString(payload).getAsJsonObject();
        JsonObject dataObject = rootObject.getAsJsonObject("data").getAsJsonObject("object");
        String eventType = event.getType();

        switch (eventType) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted(dataObject);
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(dataObject);
            case "checkout.session.expired" -> handleOrderCancellation(dataObject, "Stripe checkout session expired");
            case "payment_intent.payment_failed" -> handleOrderCancellation(dataObject, "Stripe payment failed");
            default -> log.debug("Unhandled Stripe event type: {}", eventType);
        }

        processedWebhookEventRepository.save(ProcessedWebhookEvent.builder()
                .eventId(event.getId())
                .eventType(eventType)
                .build());

        return ResponseEntity.ok("Webhook processed");
    }

    private void handleCheckoutSessionCompleted(JsonObject sessionNode) {
        String paymentStatus = getString(sessionNode, "payment_status");
        if (!"paid".equalsIgnoreCase(paymentStatus)) {
            log.info("Checkout session completed without 'paid' status: {}", paymentStatus);
            return;
        }

        Order order = resolveOrder(sessionNode).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        long expectedCents = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        long amountTotal = sessionNode.has("amount_total") && !sessionNode.get("amount_total").isJsonNull()
                ? sessionNode.get("amount_total").getAsLong() : -1L;
        String currency = getString(sessionNode, "currency");

        if (amountTotal != expectedCents || !expectedCurrency.equalsIgnoreCase(currency)) {
            log.error("Amount/currency mismatch on order {}: expected {} {}, got {} {}",
                    order.getOrderNumber(), expectedCents, expectedCurrency, amountTotal, currency);
            return;
        }

        confirmOrder(order);
    }

    private void handlePaymentIntentSucceeded(JsonObject intentNode) {
        Order order = resolveOrder(intentNode).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        long expectedCents = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        long amountReceived = intentNode.has("amount_received") && !intentNode.get("amount_received").isJsonNull()
                ? intentNode.get("amount_received").getAsLong() : -1L;
        String currency = getString(intentNode, "currency");

        if (amountReceived != expectedCents || !expectedCurrency.equalsIgnoreCase(currency)) {
            log.error("PaymentIntent amount/currency mismatch on order {}: expected {} {}, got {} {}",
                    order.getOrderNumber(), expectedCents, expectedCurrency, amountReceived, currency);
            return;
        }

        confirmOrder(order);
    }

    private void handleOrderCancellation(JsonObject dataObject, String reason) {
        resolveOrder(dataObject).ifPresent(order ->
                orderService.cancelPendingOrderAndRestoreStock(order, reason));
    }

    private Optional<Order> resolveOrder(JsonObject dataObject) {
        if (dataObject.has("metadata") && dataObject.get("metadata").isJsonObject()) {
            JsonObject metadata = dataObject.getAsJsonObject("metadata");
            String orderIdStr = getString(metadata, "orderId");
            if (orderIdStr != null && !orderIdStr.isBlank() && !"null".equals(orderIdStr)) {
                try {
                    return orderRepository.findByIdWithItems(Long.valueOf(orderIdStr));
                } catch (NumberFormatException ignored) {
                    log.warn("Invalid orderId metadata in Stripe webhook: {}", orderIdStr);
                }
            }
        }

        String objectId = getString(dataObject, "id");
        if (objectId != null && !objectId.isBlank()) {
            return orderRepository.findByPaymentIntentIdWithItems(objectId);
        }

        return Optional.empty();
    }

    private void confirmOrder(Order order) {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaidAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Order confirmed via Stripe webhook: {}", saved.getOrderNumber());

        auditService.logSystem(
                "STRIPE_WEBHOOK",
                "ORDER_PAYMENT_CONFIRMED",
                "ORDER",
                saved.getId(),
                OrderStatus.PENDING.name(),
                OrderStatus.CONFIRMED.name(),
                "Payment verified via Stripe for order " + saved.getOrderNumber()
        );

        notificationProducer.sendNotification(new NotificationMessage(
                saved.getUser().getEmail(),
                "Order Confirmed - " + saved.getOrderNumber(),
                "Great news! Your payment was successful.\n\n" +
                        "Order Number: " + saved.getOrderNumber() + "\n" +
                        "Total Amount: $" + saved.getTotalAmount() + "\n" +
                        "We will notify you once your order is shipped."));

        realtimeNotificationService.notifyOrderUpdate(
                saved,
                "Payment confirmed for order " + saved.getOrderNumber() + "!",
                "PAYMENT_CONFIRMED"
        );
    }

    private String getString(JsonObject obj, String field) {
        if (obj != null && obj.has(field) && !obj.get(field).isJsonNull()) {
            return obj.get(field).getAsString();
        }
        return null;
    }
}