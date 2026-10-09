package com.example.ecommerce.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public PaymentIntent createPaymentIntent(Long amount, String currency, Long orderId) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .putMetadata("orderId", orderId.toString()) // <-- استخدام putMetadata بدل setMetadata
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();
        return PaymentIntent.create(params);
    }

    
        public com.stripe.model.checkout.Session createCheckoutSession(
            java.util.List<com.example.ecommerce.entity.CartItem> cartItems,
            Long orderId,
            String successUrl,
            String cancelUrl) throws StripeException {

        java.util.List<com.stripe.param.checkout.SessionCreateParams.LineItem> lineItems =
                new java.util.ArrayList<>();

        for (com.example.ecommerce.entity.CartItem cartItem : cartItems) {
            var product = cartItem.getProduct();
            lineItems.add(
                com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                    .setQuantity(Long.valueOf(cartItem.getQuantity()))
                    .setPriceData(
                        com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(product.getPrice().multiply(java.math.BigDecimal.valueOf(100)).longValue())
                            .setProductData(
                                com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(product.getName())
                                    .setDescription(product.getDescription() != null ? product.getDescription() : "")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            );
        }

        com.stripe.param.checkout.SessionCreateParams params =
            com.stripe.param.checkout.SessionCreateParams.builder()
                .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("orderId", orderId.toString())
                .addAllLineItem(lineItems)
                .build();

        return com.stripe.model.checkout.Session.create(params);
    }
}