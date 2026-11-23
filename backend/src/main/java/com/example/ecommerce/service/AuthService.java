package com.example.ecommerce.service;

import com.example.ecommerce.dto.AuthResponse;
import com.example.ecommerce.dto.LoginRequest;
import com.example.ecommerce.dto.RegisterRequest;
import com.example.ecommerce.entity.RefreshToken;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.RefreshTokenRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${application.security.jwt.refresh-token-expiration}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username is already in use");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getId());
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public AuthResponse refreshToken(String refreshTokenValue) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        User user = storedToken.getUser();

        if (storedToken.isRevoked()) {
            // Reuse detection: revoke all active sessions for this user
            refreshTokenRepository.revokeAllByUserId(user.getId());
            throw new IllegalArgumentException("Refresh token has been revoked. Please log in again.");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new IllegalArgumentException("Refresh token has expired");
        }

        if (!user.isEnabled()) {
            refreshTokenRepository.revokeAllByUserId(user.getId());
            throw new IllegalArgumentException("Account is disabled");
        }

        if (!jwtService.isRefreshTokenValid(refreshTokenValue, user)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Rotate: revoke old token and issue new pair
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = createRefreshToken(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getId());
    }

    @Transactional
    public void logout(String refreshTokenValue, String userEmail) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            if (token.getUser().getEmail().equals(userEmail)) {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            }
        });
    }

    private String createRefreshToken(User user) {
        String token = jwtService.generateRefreshToken(user);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }
}