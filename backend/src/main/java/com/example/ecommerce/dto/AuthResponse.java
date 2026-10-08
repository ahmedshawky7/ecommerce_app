package com.example.ecommerce.dto;

public record AuthResponse(
    String token,
    String refreshToken,
    String email,
    String username,
    Long id
) {}