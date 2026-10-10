package com.example.ecommerce.service;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.ecommerce.dto.AuthResponse;
import com.example.ecommerce.dto.LoginRequest;
import com.example.ecommerce.dto.RegisterRequest;
import com.example.ecommerce.entity.RefreshToken;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.RefreshTokenRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtService;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AuthenticationManager authenticationManager, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if the email or username already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username is already in use");
        }

        var user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        var accessToken = jwtService.generateToken(user);
        var refreshToken = createRefreshToken(user);
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

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        var accessToken = jwtService.generateToken(user);
        var refreshToken = createRefreshToken(user);
        return new AuthResponse(
        accessToken,
        refreshToken,
        user.getEmail(),
        user.getDisplayName(),
        user.getRole().name(),
        user.getId());
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());

        var token = jwtService.generateRefreshToken(user);
        var refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        // 1. جيب التوكن من الداتابيز
        var storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // 2. اتأكد إنه مش منتهي ولا ملغي
        if (storedToken.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        // 3. جيب المستخدم
        var user = storedToken.getUser();

        // 4. اتأكد إن التوكن نفسه صالح (JWT validation)
        if (!jwtService.isTokenValid(refreshTokenValue, user)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // 5. ولّد Access Token جديد
        var newAccessToken = jwtService.generateToken(user);

        return new AuthResponse(
                newAccessToken,
                refreshTokenValue, 
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getId());
    }
}
