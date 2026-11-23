package com.example.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;
    private Long productCount;
}