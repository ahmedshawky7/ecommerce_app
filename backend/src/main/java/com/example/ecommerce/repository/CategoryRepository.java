package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT c.id, c.name, c.description, c.imageUrl, c.createdAt, " +
           "SUM(CASE WHEN p.isActive = true THEN 1L ELSE 0L END) " +
           "FROM Category c LEFT JOIN c.products p " +
           "GROUP BY c.id, c.name, c.description, c.imageUrl, c.createdAt " +
           "ORDER BY c.id ASC")
    List<Object[]> findAllWithActiveProductCountRaw();
}