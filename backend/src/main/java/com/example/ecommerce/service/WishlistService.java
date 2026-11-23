package com.example.ecommerce.service;

import com.example.ecommerce.dto.WishlistItemResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.WishlistItem;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String userEmail) {
        User user = getUser(userEmail);
        return wishlistRepository.findByUserIdWithProduct(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getWishlistProductIds(String userEmail) {
        User user = getUser(userEmail);
        return wishlistRepository.findProductIdsByUserId(user.getId());
    }

    @Transactional
    public WishlistItemResponse addToWishlist(String userEmail, Long productId) {
        User user = getUser(userEmail);
        WishlistItem existing = wishlistRepository.findByUserIdAndProductId(user.getId(), productId).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        Product product = productRepository.findByIdWithDetails(productId);
        if (product == null || !product.isActive()) {
            throw new IllegalArgumentException("Product not found");
        }

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();

        return toResponse(wishlistRepository.save(item));
    }

    @Transactional
    public void removeFromWishlist(String userEmail, Long productId) {
        User user = getUser(userEmail);
        wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .ifPresent(wishlistRepository::delete);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Product p = item.getProduct();
        return new WishlistItemResponse(
                item.getId(),
                p.getId(),
                p.getName(),
                p.getImageUrl(),
                p.getPrice(),
                p.getAverageRating(),
                p.getReviewCount(),
                p.getStockQuantity(),
                p.getCategory().getName(),
                item.getCreatedAt()
        );
    }
}