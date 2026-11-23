package com.example.ecommerce.repository;

import com.example.ecommerce.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product WHERE v.id IN :ids ORDER BY v.id ASC")
    List<ProductVariant> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    Optional<ProductVariant> findFirstByProductIdAndIsActiveTrueOrderByIdAsc(Long productId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);
}