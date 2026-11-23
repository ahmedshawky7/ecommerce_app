package com.example.ecommerce.dto;

import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        String label,
        String fullName,
        String phone,
        String streetAddress,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault,
        LocalDateTime createdAt
) {}