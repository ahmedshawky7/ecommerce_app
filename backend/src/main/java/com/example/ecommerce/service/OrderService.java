package com.example.ecommerce.service;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Gson GSON = new Gson();

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationProducer notificationProducer;
    private final RealtimeNotificationService realtimeNotificationService;
    private final AuditService auditService;
    private final PricingService pricingService;
    private final StripeService stripeService;
    private final CacheManager cacheManager;

    @Value("${app.order.pending-expiration-minutes:30}")
    private long pendingExpirationMinutes;

    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public CheckoutResponse checkout(String userEmail, CheckoutRequest request, String idempotencyKey) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String endpoint = "POST:/api/orders/checkout";
        if (hasText(idempotencyKey)) {
            Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                    .findByIdempotencyKeyAndUserIdAndEndpoint(idempotencyKey.trim(), user.getId(), endpoint);
            if (existing.isPresent()) {
                return GSON.fromJson(existing.get().getResponseBody(), CheckoutResponse.class);
            }
        }

        Cart cart = loadNonEmptyCart(user.getId());
        LockedInventory locked = lockAndValidateStock(cart.getItems());

        String paymentMethod = hasText(request.paymentMethod())
                ? request.paymentMethod().trim().toUpperCase()
                : "STRIPE";
        boolean isCod = "CASH_ON_DELIVERY".equals(paymentMethod);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.shippingAddress())
                .phone(request.phone())
                .paymentMethod(paymentMethod)
                .build();

        BigDecimal subtotal = populateOrderItemsAndDeductStock(order, cart.getItems(), locked);

        Coupon lockedCoupon = pricingService.validateAndLockCoupon(request.couponCode(), subtotal, user.getId());
        PricingBreakdownResponse pricing = pricingService.buildBreakdown(subtotal, lockedCoupon);

        order.setSubtotalAmount(pricing.subtotal());
        order.setDiscountAmount(pricing.discountAmount());
        order.setShippingAmount(pricing.shippingAmount());
        order.setTaxAmount(pricing.taxAmount());
        order.setCouponCode(pricing.appliedCouponCode());
        order.setTotalAmount(pricing.totalAmount());

        boolean isFreeOrder = pricing.totalAmount().compareTo(BigDecimal.ZERO) == 0;
        if (isCod || isFreeOrder) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaidAt(LocalDateTime.now());
            if (isFreeOrder) {
                order.setPaymentMethod("FREE_COUPON");
            }
        } else {
            order.setExpiresAt(LocalDateTime.now().plusMinutes(pendingExpirationMinutes));
        }

        Order savedOrder = orderRepository.save(order);
        recordCouponUsage(lockedCoupon, user, savedOrder.getId());
        clearUserCart(cart);

        CheckoutResponse response;
        if (isCod || isFreeOrder) {
            notificationProducer.sendNotification(new NotificationMessage(
                    user.getEmail(),
                    "Order Confirmed - " + savedOrder.getOrderNumber(),
                    "Thank you for your order!\n\nOrder Number: " + savedOrder.getOrderNumber()
                            + "\nPayment Method: " + savedOrder.getPaymentMethod()
                            + "\nTotal Amount: $" + savedOrder.getTotalAmount()
            ));
            realtimeNotificationService.notifyOrderUpdate(
                    savedOrder,
                    "Your order " + savedOrder.getOrderNumber() + " has been confirmed!",
                    "NEW_ORDER"
            );
            response = buildCheckoutResponse(savedOrder, null);
        } else {
            try {
                long amountInCents = savedOrder.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
                var paymentIntent = stripeService.createPaymentIntent(amountInCents, savedOrder.getId());
                savedOrder.setPaymentIntentId(paymentIntent.getId());
                orderRepository.save(savedOrder);

                realtimeNotificationService.notifyOrderUpdate(
                        savedOrder,
                        "Order " + savedOrder.getOrderNumber() + " created, awaiting payment.",
                        "NEW_ORDER"
                );
                response = buildCheckoutResponse(savedOrder, paymentIntent.getClientSecret());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create payment intent", e);
            }
        }

        locked.productsById().values().forEach(realtimeNotificationService::notifyLowStockIfNeeded);
        saveIdempotencyRecord(idempotencyKey, user.getId(), endpoint, response);
        evictProductsCache();
        return response;
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public CheckoutSessionResponse createCheckoutSession(
            String userEmail,
            CheckoutRequest request,
            String successUrl,
            String cancelUrl,
            String idempotencyKey) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String endpoint = "POST:/api/orders/create-checkout-session";
        if (hasText(idempotencyKey)) {
            Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                    .findByIdempotencyKeyAndUserIdAndEndpoint(idempotencyKey.trim(), user.getId(), endpoint);
            if (existing.isPresent()) {
                return GSON.fromJson(existing.get().getResponseBody(), CheckoutSessionResponse.class);
            }
        }

        Cart cart = loadNonEmptyCart(user.getId());
        LockedInventory locked = lockAndValidateStock(cart.getItems());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.shippingAddress())
                .phone(request.phone())
                .paymentMethod("STRIPE")
                .expiresAt(LocalDateTime.now().plusMinutes(pendingExpirationMinutes))
                .build();

        BigDecimal subtotal = populateOrderItemsAndDeductStock(order, cart.getItems(), locked);

        Coupon lockedCoupon = pricingService.validateAndLockCoupon(request.couponCode(), subtotal, user.getId());
        PricingBreakdownResponse pricing = pricingService.buildBreakdown(subtotal, lockedCoupon);

        order.setSubtotalAmount(pricing.subtotal());
        order.setDiscountAmount(pricing.discountAmount());
        order.setShippingAmount(pricing.shippingAmount());
        order.setTaxAmount(pricing.taxAmount());
        order.setCouponCode(pricing.appliedCouponCode());
        order.setTotalAmount(pricing.totalAmount());

        if (pricing.totalAmount().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaidAt(LocalDateTime.now());
            order.setExpiresAt(null);
            order.setPaymentMethod("FREE_COUPON");
            Order savedOrder = orderRepository.save(order);
            recordCouponUsage(lockedCoupon, user, savedOrder.getId());
            clearUserCart(cart);
            evictProductsCache();

            realtimeNotificationService.notifyOrderUpdate(
                    savedOrder,
                    "Your order " + savedOrder.getOrderNumber() + " has been confirmed!",
                    "NEW_ORDER"
            );
            locked.productsById().values().forEach(realtimeNotificationService::notifyLowStockIfNeeded);

            CheckoutSessionResponse freeResponse = new CheckoutSessionResponse(
                    null,
                    successUrl,
                    savedOrder.getId(),
                    savedOrder.getOrderNumber());
            saveIdempotencyRecord(idempotencyKey, user.getId(), endpoint, freeResponse);
            return freeResponse;
        }

        Order savedOrder = orderRepository.save(order);
        recordCouponUsage(lockedCoupon, user, savedOrder.getId());

        try {
            var session = stripeService.createCheckoutSession(savedOrder, successUrl, cancelUrl);
            savedOrder.setPaymentIntentId(session.getId());
            orderRepository.save(savedOrder);

            clearUserCart(cart);
            locked.productsById().values().forEach(realtimeNotificationService::notifyLowStockIfNeeded);

            CheckoutSessionResponse response = new CheckoutSessionResponse(
                    session.getId(),
                    session.getUrl(),
                    savedOrder.getId(),
                    savedOrder.getOrderNumber());

            saveIdempotencyRecord(idempotencyKey, user.getId(), endpoint, response);
            evictProductsCache();
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create checkout session", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Page<Long> idPage = orderRepository.findOrderIdsByUserId(user.getId(), pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, Order> ordersById = orderRepository.findAllByIdInWithItems(idPage.getContent()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        List<OrderResponse> responses = idPage.getContent().stream()
                .map(ordersById::get)
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String userEmail) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You don't have access to this order");
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, String userEmail) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You don't have access to this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be cancelled");
        }

        restoreOrderStockAndCoupon(order);
        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        evictProductsCache();

        auditService.log(
                "ORDER_CANCELLED",
                "ORDER",
                updated.getId(),
                OrderStatus.PENDING.name(),
                OrderStatus.CANCELLED.name(),
                "Order " + updated.getOrderNumber() + " cancelled by customer"
        );
        realtimeNotificationService.notifyOrderUpdate(
                updated,
                "Order " + updated.getOrderNumber() + " has been cancelled.",
                "ORDER_CANCELLED"
        );

        return toResponse(updated);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        OrderStatus currentStatus = order.getStatus();
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreOrderStockAndCoupon(order);
            evictProductsCache();
        } else if (newStatus == OrderStatus.CONFIRMED && order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        auditService.log(
                "ORDER_STATUS_UPDATED",
                "ORDER",
                updated.getId(),
                currentStatus.name(),
                newStatus.name(),
                "Order " + updated.getOrderNumber() + " updated by admin"
        );

        notificationProducer.sendNotification(new NotificationMessage(
                order.getUser().getEmail(),
                "Order Update - " + order.getOrderNumber(),
                "Your order " + order.getOrderNumber() + " status is now: " + newStatus
        ));
        realtimeNotificationService.notifyOrderUpdate(
                updated,
                "Order " + updated.getOrderNumber() + " is now " + newStatus,
                null
        );

        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Long> idPage = orderRepository.findAllOrderIds(pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, Order> ordersById = orderRepository.findAllByIdInWithItems(idPage.getContent()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        List<OrderResponse> responses = idPage.getContent().stream()
                .map(ordersById::get)
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, idPage.getTotalElements());
    }

    @Transactional
    public void cancelPendingOrderAndRestoreStock(Order order, String reason) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        restoreOrderStockAndCoupon(order);
        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        evictProductsCache();

        auditService.logSystem(
                "SYSTEM",
                "ORDER_AUTO_CANCELLED",
                "ORDER",
                updated.getId(),
                OrderStatus.PENDING.name(),
                OrderStatus.CANCELLED.name(),
                "Order " + updated.getOrderNumber() + " cancelled: " + reason
        );
        realtimeNotificationService.notifyOrderUpdate(
                updated,
                "Order " + updated.getOrderNumber() + " was cancelled (" + reason + ").",
                "ORDER_CANCELLED"
        );
        log.info("Order {} cancelled ({}), stock restored", order.getOrderNumber(), reason);
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void cleanupExpiredOrdersAndRecords() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findExpiredPendingStripeOrders(OrderStatus.PENDING, now);
        for (Order order : expiredOrders) {
            cancelPendingOrderAndRestoreStock(order, "checkout expired");
        }
        idempotencyRecordRepository.deleteExpired(now);
        refreshTokenRepository.deleteExpiredOrRevoked(now);
    }

    // ============ Helpers ============

    private record LockedInventory(Map<Long, Product> productsById, Map<Long, ProductVariant> variantsById) {}

    private Cart loadNonEmptyCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        return cart;
    }

    private LockedInventory lockAndValidateStock(List<CartItem> items) {
        List<Long> sortedProductIds = items.stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();
        List<Long> sortedVariantIds = items.stream()
                .map(item -> item.getVariant().getId())
                .distinct()
                .sorted()
                .toList();

        Map<Long, Product> lockedProducts = productRepository.findAllByIdInForUpdate(sortedProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, ProductVariant> lockedVariants = productVariantRepository.findAllByIdInForUpdate(sortedVariantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        for (CartItem item : items) {
            Product product = lockedProducts.get(item.getProduct().getId());
            ProductVariant variant = lockedVariants.get(item.getVariant().getId());
            if (product == null || !product.isActive() || variant == null || !variant.isActive()) {
                throw new IllegalArgumentException("Product '" + item.getProduct().getName() + "' is not available");
            }
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Not enough stock for '" + product.getName() + "'. Available: " + variant.getStockQuantity());
            }
        }
        return new LockedInventory(lockedProducts, lockedVariants);
    }

    private BigDecimal populateOrderItemsAndDeductStock(
            Order order, List<CartItem> cartItems, LockedInventory locked) {
        BigDecimal subtotalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = locked.productsById().get(cartItem.getProduct().getId());
            ProductVariant variant = locked.variantsById().get(cartItem.getVariant().getId());

            BigDecimal unitPrice = variant.getPrice();
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            String variantLabel = formatVariantLabel(variant);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .variant(variant)
                    .productName(product.getName())
                    .sku(variant.getSku())
                    .variantLabel(variantLabel)
                    .unitPrice(unitPrice)
                    .quantity(cartItem.getQuantity())
                    .subtotal(itemSubtotal)
                    .build();

            order.getItems().add(orderItem);
            subtotalAmount = subtotalAmount.add(itemSubtotal);

            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            productVariantRepository.save(variant);

            product.setStockQuantity(Math.max(0, product.getStockQuantity() - cartItem.getQuantity()));
            productRepository.save(product);
        }
        return subtotalAmount;
    }

    private void restoreOrderStockAndCoupon(Order order) {
        List<Long> sortedProductIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();
        List<Long> sortedVariantIds = order.getItems().stream()
                .map(OrderItem::getVariant)
                .filter(Objects::nonNull)
                .map(ProductVariant::getId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, Product> lockedProducts = productRepository.findAllByIdInForUpdate(sortedProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, ProductVariant> lockedVariants = sortedVariantIds.isEmpty()
                ? Map.of()
                : productVariantRepository.findAllByIdInForUpdate(sortedVariantIds).stream()
                        .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null) {
                ProductVariant variant = lockedVariants.get(item.getVariant().getId());
                if (variant != null) {
                    variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                    productVariantRepository.save(variant);
                }
            }
            Product product = lockedProducts.get(item.getProduct().getId());
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        if (hasText(order.getCouponCode())) {
            couponUsageRepository.findByOrderId(order.getId()).ifPresent(usage -> {
                couponRepository.findByCodeForUpdate(order.getCouponCode()).ifPresent(coupon -> {
                    coupon.setUsageCount(Math.max(0, coupon.getUsageCount() - 1));
                    couponRepository.save(coupon);
                });
                couponUsageRepository.delete(usage);
            });
        }
    }

    private void recordCouponUsage(Coupon coupon, User user, Long orderId) {
        if (coupon == null) {
            return;
        }
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);

        couponUsageRepository.save(CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .orderId(orderId)
                .build());
    }

    private String formatVariantLabel(ProductVariant variant) {
        List<String> parts = new ArrayList<>();
        if (hasText(variant.getSize())) parts.add(variant.getSize());
        if (hasText(variant.getColor())) parts.add(variant.getColor());
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private void clearUserCart(Cart cart) {
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private void saveIdempotencyRecord(String key, Long userId, String endpoint, Object responseObj) {
        if (!hasText(key)) {
            return;
        }
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(key.trim())
                .userId(userId)
                .endpoint(endpoint)
                .responseBody(GSON.toJson(responseObj))
                .build();
        idempotencyRecordRepository.save(record);
    }

    public void evictProductsCache() {
        Cache productsCache = cacheManager.getCache("products");
        if (productsCache != null) {
            productsCache.clear();
        }
    }

    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private CheckoutResponse buildCheckoutResponse(Order order, String clientSecret) {
        return new CheckoutResponse(
                order.getId(),
                order.getOrderNumber(),
                clientSecret,
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getShippingAmount(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getCouponCode());
    }

    private OrderResponse toResponse(Order order) {
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

        return new OrderResponse(
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
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}