package com.example.ecommerce.controller;

import com.example.ecommerce.dto.WishlistItemResponse;
import com.example.ecommerce.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getWishlist(authentication.getName()));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getWishlistProductIds(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getWishlistProductIds(authentication.getName()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistItemResponse> addToWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        return ResponseEntity.ok(wishlistService.addToWishlist(authentication.getName(), productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        wishlistService.removeFromWishlist(authentication.getName(), productId);
        return ResponseEntity.noContent().build();
    }
}