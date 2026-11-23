
package com.example.ecommerce.service;

import com.example.ecommerce.dto.*;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Cacheable(value = "products", key = "'all-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findAllActiveWithDetails(pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Cacheable(value = "products", key = "'filter-' + #keyword + '-' + #categoryId + '-' + #minPrice + '-' + #maxPrice + '-' + #minRating + '-' + #inStockOnly + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> filterProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            Boolean inStockOnly,
            Pageable pageable) {

        Specification<Product> spec = ProductSpecification.withFilters(
                keyword, categoryId, minPrice, maxPrice, minRating, inStockOnly);

        Page<ProductResponse> page = productRepository.findAll(spec, pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Cacheable(value = "products", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithDetails(id);
        if (product == null || !product.isActive()) {
            throw new IllegalArgumentException("Product not found");
        }
        return toResponse(product);
    }

    @Cacheable(value = "products", key = "'category-' + #categoryId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<ProductResponse> page = productRepository.findByCategoryWithDetails(categoryId, pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Cacheable(value = "products", key = "'search-' + #keyword.trim().toLowerCase() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        Page<ProductResponse> page = productRepository.searchByNameWithDetails(keyword.trim(), pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "categories", allEntries = true)
    })
    public ProductResponse createProduct(ProductRequest request, String adminEmail) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .imageUrl(request.imageUrl())
                .category(category)
                .seller(admin)
                .isActive(true)
                .build();

        applyImages(product, request);
        applyVariantsForCreate(product, request);
        syncProductSummaryFromVariants(product);

        Product saved = productRepository.save(product);

        auditService.log(
                "PRODUCT_CREATED",
                "PRODUCT",
                saved.getId(),
                null,
                "price=" + saved.getPrice() + ", stock=" + saved.getStockQuantity(),
                "Created product: " + saved.getName());

        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "carts", allEntries = true)
    })
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findByIdWithDetails(id);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }

        BigDecimal oldPrice = product.getPrice();
        Integer oldStock = product.getStockQuantity();

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);

        applyImages(product, request);
        applyVariantsForUpdate(product, request);
        syncProductSummaryFromVariants(product);

        Product updated = productRepository.save(product);

        if (oldPrice.compareTo(updated.getPrice()) != 0) {
            auditService.log(
                    "PRODUCT_PRICE_UPDATED",
                    "PRODUCT",
                    updated.getId(),
                    oldPrice.toString(),
                    updated.getPrice().toString(),
                    "Price changed for " + updated.getName());
        }
        if (!Objects.equals(oldStock, updated.getStockQuantity())) {
            auditService.log(
                    "PRODUCT_STOCK_UPDATED",
                    "PRODUCT",
                    updated.getId(),
                    String.valueOf(oldStock),
                    String.valueOf(updated.getStockQuantity()),
                    "Stock adjusted for " + updated.getName());
        }

        return toResponse(updated);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "carts", allEntries = true)
    })
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        product.setActive(false);
        for (ProductVariant variant : product.getVariants()) {
            variant.setActive(false);
        }
        productRepository.save(product);

        auditService.log(
                "PRODUCT_DELETED",
                "PRODUCT",
                product.getId(),
                "active=true",
                "active=false",
                "Soft-deleted product: " + product.getName());
    }

    private void applyImages(Product product, ProductRequest request) {
        if (request.images() == null) {
            return;
        }
        product.getImages().clear();
        String primaryUrl = null;
        int idx = 0;
        for (ProductImageRequest imgReq : request.images()) {
            ProductImage img = ProductImage.builder()
                    .product(product)
                    .imageUrl(imgReq.imageUrl())
                    .altText(imgReq.altText())
                    .displayOrder(imgReq.displayOrder() != null ? imgReq.displayOrder() : idx)
                    .isPrimary(imgReq.isPrimary())
                    .build();
            product.getImages().add(img);
            if (imgReq.isPrimary() || primaryUrl == null) {
                primaryUrl = imgReq.imageUrl();
            }
            idx++;
        }
        if (primaryUrl != null) {
            product.setImageUrl(primaryUrl);
        }
    }

    private void applyVariantsForCreate(Product product, ProductRequest request) {
        if (request.variants() == null || request.variants().isEmpty()) {
            String autoSku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            product.getVariants().add(ProductVariant.builder()
                    .product(product)
                    .sku(autoSku)
                    .size("Standard")
                    .color("Default")
                    .price(request.price())
                    .stockQuantity(request.stockQuantity())
                    .imageUrl(product.getImageUrl())
                    .isActive(true)
                    .build());
            return;
        }

        Set<String> seenSkus = new HashSet<>();
        for (ProductVariantRequest varReq : request.variants()) {
            String sku = varReq.sku().trim();
            if (!seenSkus.add(sku) || productVariantRepository.existsBySku(sku)) {
                throw new IllegalArgumentException("SKU already exists: " + sku);
            }
            product.getVariants().add(ProductVariant.builder()
                    .product(product)
                    .sku(sku)
                    .size(varReq.size())
                    .color(varReq.color())
                    .price(varReq.price())
                    .stockQuantity(varReq.stockQuantity())
                    .imageUrl(varReq.imageUrl())
                    .isActive(true)
                    .build());
        }
    }

    private void applyVariantsForUpdate(Product product, ProductRequest request) {
        if (request.variants() == null || request.variants().isEmpty()) {
            ProductVariant defaultVar = product.getVariants().stream()
                    .filter(ProductVariant::isActive)
                    .findFirst()
                    .orElse(null);
            if (defaultVar != null) {
                defaultVar.setPrice(request.price());
                defaultVar.setStockQuantity(request.stockQuantity());
                if (product.getImageUrl() != null) {
                    defaultVar.setImageUrl(product.getImageUrl());
                }
            } else {
                String autoSku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                product.getVariants().add(ProductVariant.builder()
                        .product(product)
                        .sku(autoSku)
                        .size("Standard")
                        .color("Default")
                        .price(request.price())
                        .stockQuantity(request.stockQuantity())
                        .imageUrl(product.getImageUrl())
                        .isActive(true)
                        .build());
            }
            return;
        }

        Map<Long, ProductVariant> existingById = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        Set<Long> retainedIds = new HashSet<>();
        Set<String> seenSkus = new HashSet<>();

        for (ProductVariantRequest varReq : request.variants()) {
            String sku = varReq.sku().trim();
            if (!seenSkus.add(sku)) {
                throw new IllegalArgumentException("Duplicate SKU in request: " + sku);
            }

            if (varReq.id() != null && existingById.containsKey(varReq.id())) {
                if (productVariantRepository.existsBySkuAndIdNot(sku, varReq.id())) {
                    throw new IllegalArgumentException("SKU already exists: " + sku);
                }
                ProductVariant existing = existingById.get(varReq.id());
                existing.setSku(sku);
                existing.setSize(varReq.size());
                existing.setColor(varReq.color());
                existing.setPrice(varReq.price());
                existing.setStockQuantity(varReq.stockQuantity());
                existing.setImageUrl(varReq.imageUrl());
                existing.setActive(true);
                retainedIds.add(existing.getId());
            } else {
                if (productVariantRepository.existsBySku(sku)) {
                    throw new IllegalArgumentException("SKU already exists: " + sku);
                }
                product.getVariants().add(ProductVariant.builder()
                        .product(product)
                        .sku(sku)
                        .size(varReq.size())
                        .color(varReq.color())
                        .price(varReq.price())
                        .stockQuantity(varReq.stockQuantity())
                        .imageUrl(varReq.imageUrl())
                        .isActive(true)
                        .build());
            }
        }

        for (ProductVariant existing : product.getVariants()) {
            if (existing.getId() != null && !retainedIds.contains(existing.getId())) {
                existing.setActive(false);
            }
        }
    }

    public void syncProductSummaryFromVariants(Product product) {
        List<ProductVariant> activeVariants = product.getVariants().stream()
                .filter(ProductVariant::isActive)
                .toList();

        if (!activeVariants.isEmpty()) {
            BigDecimal minPrice = activeVariants.stream()
                    .map(ProductVariant::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(product.getPrice());
            int totalStock = activeVariants.stream()
                    .mapToInt(ProductVariant::getStockQuantity)
                    .sum();
            product.setPrice(minPrice);
            product.setStockQuantity(totalStock);
        }
    }

    private ProductResponse toResponse(Product product) {
        List<ProductVariantResponse> variantResponses = new ArrayList<>();
        for (ProductVariant v : product.getVariants()) {
            if (v.isActive()) {
                variantResponses.add(ProductVariantResponse.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .size(v.getSize())
                        .color(v.getColor())
                        .price(v.getPrice())
                        .stockQuantity(v.getStockQuantity())
                        .imageUrl(v.getImageUrl())
                        .active(v.isActive())
                        .build());
            }
        }

        List<ProductImageResponse> imageResponses = new ArrayList<>();
        for (ProductImage img : product.getImages()) {
            imageResponses.add(ProductImageResponse.builder()
                    .id(img.getId())
                    .imageUrl(img.getImageUrl())
                    .altText(img.getAltText())
                    .displayOrder(img.getDisplayOrder())
                    .primary(img.isPrimary())
                    .build());
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .active(product.isActive())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getDisplayName())
                .variants(variantResponses)
                .images(imageResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
