package com.example.ecommerce.security;

import com.example.ecommerce.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${application.security.jwt.secret-key}") String secretKeyString,
            @Value("${application.security.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${application.security.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyString));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TYPE_ACCESS);
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());
        return buildToken(claims, user, accessTokenExpiration);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TYPE_REFRESH);
        return buildToken(claims, user, refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        String tokenType = claims.get(CLAIM_TYPE, String.class);
        return TYPE_ACCESS.equals(tokenType)
                && claims.getSubject().equals(userDetails.getUsername())
                && !claims.getExpiration().before(new Date());
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        String tokenType = claims.get(CLAIM_TYPE, String.class);
        return TYPE_REFRESH.equals(tokenType)
                && claims.getSubject().equals(userDetails.getUsername())
                && !claims.getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isAccessTokenValid(token, userDetails);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}