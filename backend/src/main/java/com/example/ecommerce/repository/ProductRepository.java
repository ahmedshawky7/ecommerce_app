package com.example.ecommerce.repository;

import com.example.ecommerce.dto.LowStockProductResponse;
import com.example.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "seller"})
    Page<Product> findAll(@Nullable Specification<Product> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id ASC")
    List<Product> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    @Query("SELECT new com.example.ecommerce.dto.LowStockProductResponse(" +
           "p.id, p.name, p.stockQuantity, p.price) " +
           "FROM Product p " +
           "WHERE p.isActive = true AND p.stockQuantity <= :threshold " +
           "ORDER BY p.stockQuantity ASC")
    List<LowStockProductResponse> findLowStockProducts(@Param("threshold") Integer threshold, Pageable pageable);

    @Query(value = "SELECT p FROM Product p " +
                   "JOIN FETCH p.category " +
                   "JOIN FETCH p.seller " +
                   "WHERE p.isActive = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
    Page<Product> findAllActiveWithDetails(Pageable pageable);

    @Query(value = "SELECT p FROM Product p " +
                   "JOIN FETCH p.category " +
                   "JOIN FETCH p.seller " +
                   "WHERE p.category.id = :categoryId AND p.isActive = true",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    Page<Product> findByCategoryWithDetails(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = "SELECT p FROM Product p " +
                   "JOIN FETCH p.category " +
                   "JOIN FETCH p.seller " +
                   "WHERE p.isActive = true " +
                   "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByNameWithDetails(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p " +
           "JOIN FETCH p.category " +
           "JOIN FETCH p.seller " +
           "WHERE p.id = :id")
    Product findByIdWithDetails(@Param("id") Long id);

    long countByCategoryId(Long categoryId);

    long countByCategoryIdAndIsActiveTrue(Long categoryId);
}