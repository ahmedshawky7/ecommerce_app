package com.example.ecommerce.repository;

import com.example.ecommerce.dto.LowStockProductResponse;
import com.example.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

       // المنتجات اللي مخزونها قليل
       @Query("SELECT new com.example.ecommerce.dto.LowStockProductResponse(" +
                     "p.id, p.name, p.stockQuantity, p.price) " +
                     "FROM Product p " +
                     "WHERE p.isActive = true AND p.stockQuantity <= :threshold " +
                     "ORDER BY p.stockQuantity ASC")
       java.util.List<LowStockProductResponse> findLowStockProducts(@Param("threshold") Integer threshold,
                     Pageable pageable);

       // جلب كل المنتجات النشطة مع Category و Seller
       @Query("SELECT p FROM Product p " +
                     "JOIN FETCH p.category " +
                     "JOIN FETCH p.seller " +
                     "WHERE p.isActive = true")
       Page<Product> findAllActiveWithDetails(Pageable pageable);

       // المنتجات حسب الفئة
       @Query("SELECT p FROM Product p " +
                     "JOIN FETCH p.category " +
                     "JOIN FETCH p.seller " +
                     "WHERE p.category.id = :categoryId AND p.isActive = true")
       Page<Product> findByCategoryWithDetails(@Param("categoryId") Long categoryId, Pageable pageable);

       // البحث بالاسم
       @Query("SELECT p FROM Product p " +
                     "JOIN FETCH p.category " +
                     "JOIN FETCH p.seller " +
                     "WHERE p.isActive = true " +
                     "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Product> searchByNameWithDetails(@Param("keyword") String keyword, Pageable pageable);

       // المنتجات حسب الـ Seller
       @Query("SELECT p FROM Product p " +
                     "JOIN FETCH p.category " +
                     "JOIN FETCH p.seller " +
                     "WHERE p.seller.id = :sellerId")
       List<Product> findBySellerIdWithDetails(@Param("sellerId") Long sellerId);

       // جلب منتج معين مع التفاصيل
       @Query("SELECT p FROM Product p " +
                     "JOIN FETCH p.category " +
                     "JOIN FETCH p.seller " +
                     "WHERE p.id = :id")
       Product findByIdWithDetails(@Param("id") Long id);

       // عد المنتجات في الفئة
       long countByCategoryId(Long categoryId);
}