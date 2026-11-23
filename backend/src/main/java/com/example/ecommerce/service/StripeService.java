package com.example.ecommerce.service;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.stripe.currency:usd}")
    private String currency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public PaymentIntent createPaymentIntent(Long amountInCents, Long orderId) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                .putMetadata("orderId", orderId.toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();
        return PaymentIntent.create(params);
    }

    public Session createCheckoutSession(
            Order order,
            String successUrl,
            String cancelUrl) throws StripeException {

        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        boolean hasDiscount = order.getDiscountAmount() != null
                && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;

        if (!hasDiscount) {
            for (OrderItem item : order.getItems()) {
                String name = item.getVariantLabel() != null && !item.getVariantLabel().isBlank()
                        ? item.getProductName() + " (" + item.getVariantLabel() + ")"
                        : item.getProductName();

                lineItems.add(buildLineItem(
                        name,
                        null,
                        item.getUnitPrice(),
                        Long.valueOf(item.getQuantity())
                ));
            }
            if (order.getShippingAmount() != null && order.getShippingAmount().compareTo(BigDecimal.ZERO) > 0) {
                lineItems.add(buildLineItem("Shipping", null, order.getShippingAmount(), 1L));
            }
            if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                lineItems.add(buildLineItem("Tax", null, order.getTaxAmount(), 1L));
            }
        } else {
            String desc = "Order " + order.getOrderNumber()
                    + " (Coupon " + order.getCouponCode() + " applied, incl. shipping & tax)";
            lineItems.add(buildLineItem(
                    "Order " + order.getOrderNumber(),
                    desc,
                    order.getTotalAmount(),
                    1L
            ));
        }

        long expiresAtEpoch = Instant.now().plusSeconds(1860).getEpochSecond();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setExpiresAt(expiresAtEpoch)
                .putMetadata("orderId", order.getId().toString())
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("orderId", order.getId().toString())
                                .build()
                )
                .addAllLineItem(lineItems)
                .build();

        return Session.create(params);
    }

    private SessionCreateParams.LineItem buildLineItem(
            String name, String description, BigDecimal unitAmount, Long quantity) {

        SessionCreateParams.LineItem.PriceData.ProductData.Builder productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(name);

        if (description != null && !description.isBlank()) {
            productData.setDescription(description);
        }

        return SessionCreateParams.LineItem.builder()
                .setQuantity(quantity)
                .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(unitAmount.multiply(BigDecimal.valueOf(100)).longValue())
                                .setProductData(productData.build())
                                .build()
                )
                .build();
    }
}