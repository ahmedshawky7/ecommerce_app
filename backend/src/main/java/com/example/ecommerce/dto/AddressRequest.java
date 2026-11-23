package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Label is required (e.g., Home, Work)")
        @Size(max = 50)
        String label,

        @NotBlank(message = "Full name is required")
        @Size(max = 100)
        String fullName,

        @NotBlank(message = "Phone is required")
        @Size(max = 20)
        String phone,

        @NotBlank(message = "Street address is required")
        @Size(max = 300)
        String streetAddress,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @Size(max = 100)
        String state,

        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100)
        String country,

        boolean isDefault
) {}