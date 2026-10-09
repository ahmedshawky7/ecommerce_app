package com.example.ecommerce.service;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;
    private final StripeService stripeService;

    // ============ Checkout ============
    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public CheckoutResponse checkout(String userEmail, CheckoutRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // 1. Validate stock for ALL items first (fail fast)
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new IllegalArgumentException("Product '" + product.getName() + "' is not available");
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Not enough stock for '" + product.getName() +
                                "'. Available: " + product.getStockQuantity());
            }
        }

        // 2. Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.shippingAddress())
                .phone(request.phone())
                .paymentMethod(request.paymentMethod() != null ? request.paymentMethod() : "CASH_ON_DELIVERY")
                .build();

        // 3. Convert cart items to order items + deduct stock
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // 4. Clear cart
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cartRepository.save(cart);

        // 5. Create Stripe PaymentIntent
        try {
            // Stripe بياخد المبلغ بالسنت (مثلاً 10.00 دولار = 1000)
            long amountInCents = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
            var paymentIntent = stripeService.createPaymentIntent(amountInCents, "usd", savedOrder.getId());
            // تحديث الـ Order بالـ paymentIntentId
            savedOrder.setPaymentIntentId(paymentIntent.getId());
            orderRepository.save(savedOrder);

            // 6. (اختياري) نبعت إيميل تأكيد الطلب هنا لو الدفع COD، أو نأجله للـ Webhook لو
            // Stripe
            // هنشيل الإيميل من هنا ونخليه في الـ Webhook بعد ما الدفع ينجح
            /*
             * NotificationMessage notification = new NotificationMessage(
             * user.getEmail(),
             * "Order Confirmation - " + savedOrder.getOrderNumber(),
             * "Thank you for your order!\n\n" +
             * "Order Number: " + savedOrder.getOrderNumber() + "\n" +
             * "Total Amount: $" + savedOrder.getTotalAmount() + "\n" +
             * "Shipping Address: " + savedOrder.getShippingAddress() + "\n\n" +
             * "We will notify you once your order is shipped.");
             * notificationProducer.sendNotification(notification);
             */

            return new CheckoutResponse(
                    savedOrder.getId(),
                    savedOrder.getOrderNumber(),
                    paymentIntent.getClientSecret(),
                    savedOrder.getTotalAmount());

        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage());
        }
    }

    // ============ Get User Orders ============
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return orderRepository.findByUserIdWithItems(user.getId(), pageable)
                .map(this::toResponse);
    }

    // ============ Get Order By Id ============
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String userEmail) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // تأكد إن الطلب بتاع نفس المستخدم
        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You don't have access to this order");
        }

        return toResponse(order);
    }

    // ============ Cancel Order ============
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

        // رجّع المخزون
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        return toResponse(updated);
    }

    // ============ Admin: Update Status ============
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot update a cancelled order");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Cannot update a delivered order");
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        return toResponse(updated);
    }

    // ============ Admin: Get All Orders ============
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    // ============ Helpers ============
    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    // ============ Create Stripe Checkout Session ============
    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public com.example.ecommerce.dto.CheckoutSessionResponse createCheckoutSession(
            String userEmail,
            com.example.ecommerce.dto.CheckoutRequest request,
            String successUrl,
            String cancelUrl) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Validate stock
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new IllegalArgumentException("Product '" + product.getName() + "' is not available");
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Not enough stock for '" + product.getName() +
                                "'. Available: " + product.getStockQuantity());
            }
        }

        // Create Order (PENDING)
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.shippingAddress())
                .phone(request.phone())
                .paymentMethod("STRIPE")
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        try {
            var session = stripeService.createCheckoutSession(
                    new java.util.ArrayList<>(cart.getItems()),
                    savedOrder.getId(),
                    successUrl,
                    cancelUrl);

            // Save session ID
            savedOrder.setPaymentIntentId(session.getId());
            orderRepository.save(savedOrder);

            // Clear cart
            cartItemRepository.deleteByCartId(cart.getId());
            cart.getItems().clear();
            cartRepository.save(cart);

            return new com.example.ecommerce.dto.CheckoutSessionResponse(
                    session.getId(),
                    session.getUrl(),
                    savedOrder.getId(),
                    savedOrder.getOrderNumber());

        } catch (Exception e) {
            throw new RuntimeException("Failed to create checkout session: " + e.getMessage());
        }
    }

    private OrderResponse toResponse(Order order) {
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

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
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