package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_product_image_product", columnList = "product_id, display_order")
})
@EqualsAndHashCode(of = "id")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;
}