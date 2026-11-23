package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse implements Serializable {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private boolean active;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;
    @Builder.Default
    private List<ProductVariantResponse> variants = new ArrayList<>();
    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}