package com.example.ecommerce.dto;

import java.io.Serializable;

public record CheckoutSessionResponse(
    String sessionId,
    String sessionUrl,
    Long orderId,
    String orderNumber
) implements Serializable {}