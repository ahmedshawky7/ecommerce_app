package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
    @NotBlank(message = "Shipping address is required")
    @Size(max = 500)
    String shippingAddress,

    @NotBlank(message = "Phone is required")
    @Size(max = 20)
    String phone,

    @Size(max = 50)
    String paymentMethod   // "CASH_ON_DELIVERY" مبدئيًا
) {}