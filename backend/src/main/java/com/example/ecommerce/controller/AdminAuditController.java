package com.example.ecommerce.controller;

import com.example.ecommerce.dto.AuditLogResponse;
import com.example.ecommerce.service.AuditService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Logs", description = "Financial & Administrative Audit Logs")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actorEmail,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.searchLogs(action, entityType, actorEmail, pageable));
    }
}