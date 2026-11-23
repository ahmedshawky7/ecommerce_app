package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddToCartRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.dto.CartResponse;
import com.example.ecommerce.dto.UpdateCartItemRequest;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "carts", key = "#userEmail")
    @Transactional
    public CartResponse getCart(String userEmail) {
        Cart cart = getOrCreateCartEntity(userEmail);
        return toResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public CartResponse addToCart(String userEmail, AddToCartRequest request) {
        Cart cart = getOrCreateCartEntity(userEmail);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is not available");
        }

        ProductVariant variant;
        if (request.variantId() != null) {
            variant = productVariantRepository.findById(request.variantId())
                    .orElseThrow(() -> new IllegalArgumentException("Product variant not found"));
            if (!variant.getProduct().getId().equals(product.getId()) || !variant.isActive()) {
                throw new IllegalArgumentException("Selected variant is not available for this product");
            }
        } else {
            variant = productVariantRepository.findFirstByProductIdAndIsActiveTrueOrderByIdAsc(product.getId())
                    .orElseThrow(() -> new IllegalArgumentException("No active variants available for this product"));
        }

        if (variant.getStockQuantity() < request.quantity()) {
            throw new IllegalArgumentException("Not enough stock. Available: " + variant.getStockQuantity());
        }

        Long targetVariantId = variant.getId();
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getVariant().getId().equals(targetVariantId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.quantity();
            if (variant.getStockQuantity() < newQuantity) {
                throw new IllegalArgumentException("Not enough stock. Available: " + variant.getStockQuantity());
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.quantity())
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public CartResponse updateCartItem(String userEmail, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getCartByUserEmail(userEmail);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in your cart"));

        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();
        if (!product.isActive() || !variant.isActive()) {
            throw new IllegalArgumentException("Product variant is no longer available");
        }

        if (variant.getStockQuantity() < request.quantity()) {
            throw new IllegalArgumentException("Not enough stock. Available: " + variant.getStockQuantity());
        }

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);

        return toResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public CartResponse removeCartItem(String userEmail, Long itemId) {
        Cart cart = getCartByUserEmail(userEmail);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in your cart"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return toResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public void clearCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cartItemRepository.deleteByCartId(cart.getId());
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private Cart getCartByUserEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));
    }

    private Cart getOrCreateCartEntity(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();
            BigDecimal unitPrice = variant.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            String imageUrl = (variant.getImageUrl() != null && !variant.getImageUrl().isBlank())
                    ? variant.getImageUrl()
                    : product.getImageUrl();

            itemResponses.add(CartItemResponse.builder()
                    .id(item.getId())
                    .productId(product.getId())
                    .variantId(variant.getId())
                    .sku(variant.getSku())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .productName(product.getName())
                    .productImageUrl(imageUrl)
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .availableStock(variant.getStockQuantity())
                    .build());

            totalPrice = totalPrice.add(subtotal);
            totalItems += item.getQuantity();
        }

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }
}