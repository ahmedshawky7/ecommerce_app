package com.example.ecommerce.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        String actorRole,
        String action,
        String entityType,
        Long entityId,
        String oldValue,
        String newValue,
        String details,
        String ipAddress,
        LocalDateTime createdAt
) {}