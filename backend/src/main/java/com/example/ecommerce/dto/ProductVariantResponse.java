package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse implements Serializable {
    private Long id;
    private String sku;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private boolean active;
}