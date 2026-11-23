package com.example.ecommerce.security;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "0MoQfENiTDdfQdciJ8/g7NBwNaY0NORUtkXbmgg5cvG/shqA9Pj378YqxHixqgzG",
                900_000L,
                604_800_000L
        );
        user = User.builder()
                .id(1L)
                .username("ahmed")
                .email("ahmed@example.com")
                .password("encoded")
                .role(Role.CUSTOMER)
                .isEnabled(true)
                .build();
    }

    @Test
    void shouldEnforceTokenTypeSeparation() {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        assertTrue(jwtService.isAccessTokenValid(accessToken, user));
        assertFalse(jwtService.isRefreshTokenValid(accessToken, user));

        assertTrue(jwtService.isRefreshTokenValid(refreshToken, user));
        assertFalse(jwtService.isAccessTokenValid(refreshToken, user));
    }
}