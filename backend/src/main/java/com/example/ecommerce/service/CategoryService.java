package com.example.ecommerce.service;

import com.example.ecommerce.dto.CategoryRequest;
import com.example.ecommerce.dto.CategoryResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Object[]> rows = categoryRepository.findAllWithActiveProductCountRaw();
        List<CategoryResponse> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(CategoryResponse.builder()
                    .id(((Number) row[0]).longValue())
                    .name((String) row[1])
                    .description((String) row[2])
                    .imageUrl((String) row[3])
                    .createdAt((LocalDateTime) row[4])
                    .productCount(row[5] != null ? ((Number) row[5]).longValue() : 0L)
                    .build());
        }
        return result;
    }

    @Cacheable(value = "categories", key = "#id")
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        long activeCount = productRepository.countByCategoryIdAndIsActiveTrue(category.getId());
        return toResponse(category, activeCount);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .build();

        Category saved = categoryRepository.save(category);
        return toResponse(saved, 0L);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }

        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());

        Category updated = categoryRepository.save(category);
        long activeCount = productRepository.countByCategoryIdAndIsActiveTrue(updated.getId());
        return toResponse(updated, activeCount);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (productRepository.countByCategoryId(id) > 0) {
            throw new IllegalArgumentException("Cannot delete category with existing products");
        }

        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category, long productCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .createdAt(category.getCreatedAt())
                .productCount(productCount)
                .build();
    }
}