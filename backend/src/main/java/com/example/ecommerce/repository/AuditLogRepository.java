package com.example.ecommerce.repository;

import com.example.ecommerce.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a " +
           "WHERE (:action IS NULL OR a.action = :action) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:actorEmail IS NULL OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :actorEmail, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchLogs(
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("actorEmail") String actorEmail,
            Pageable pageable);
}