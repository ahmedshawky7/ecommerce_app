package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddToCartRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.dto.CartResponse;
import com.example.ecommerce.dto.UpdateCartItemRequest;
import com.example.ecommerce.entity.Cart;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Cacheable(value = "carts", key = "#userEmail")
    @Transactional(readOnly = true)
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
        
        if (product.getStockQuantity() < request.quantity()) {
            throw new IllegalArgumentException("Not enough stock. Available: " + product.getStockQuantity());
        }
        
        // هل المنتج موجود في السلة؟
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.productId()))
                .findFirst()
                .orElse(null);
        
        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.quantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new IllegalArgumentException("Not enough stock. Available: " + product.getStockQuantity());
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
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
        
        if (item.getProduct().getStockQuantity() < request.quantity()) {
            throw new IllegalArgumentException("Not enough stock. Available: " + item.getProduct().getStockQuantity());
        }
        
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        
        return toResponse(cart);
    }

    @Transactional
    @CacheEvict(value = "carts", key = "#userEmail")
    public void removeCartItem(String userEmail, Long itemId) {
        Cart cart = getCartByUserEmail(userEmail);
        
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in your cart"));
        
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
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
            BigDecimal subtotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            
            itemResponses.add(new CartItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getImageUrl(),
                    item.getProduct().getPrice(),
                    item.getQuantity(),
                    subtotal,
                    item.getProduct().getStockQuantity()
            ));
            
            totalPrice = totalPrice.add(subtotal);
            totalItems += item.getQuantity();
        }
        
        return new CartResponse(
                cart.getId(),
                itemResponses,
                totalItems,
                totalPrice
        );
    }
}