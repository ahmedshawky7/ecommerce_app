package com.example.ecommerce.service;

import com.example.ecommerce.dto.AuditLogResponse;
import com.example.ecommerce.entity.AuditLog;
import com.example.ecommerce.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(
            String action,
            String entityType,
            Long entityId,
            String oldValue,
            String newValue,
            String details) {

        String actorEmail = "SYSTEM";
        String actorRole = "SYSTEM";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            actorEmail = auth.getName();
            actorRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("USER");
        }

        auditLogRepository.save(AuditLog.builder()
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .details(details)
                .ipAddress(resolveClientIp())
                .build());
    }

    public void logSystem(
            String actorEmail,
            String action,
            String entityType,
            Long entityId,
            String oldValue,
            String newValue,
            String details) {

        auditLogRepository.save(AuditLog.builder()
                .actorEmail(actorEmail)
                .actorRole("SYSTEM")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .details(details)
                .ipAddress(resolveClientIp())
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> searchLogs(
            String action, String entityType, String actorEmail, Pageable pageable) {
        String normAction = normalize(action);
        String normEntity = normalize(entityType);
        String normActor = normalize(actorEmail);

        return auditLogRepository.searchLogs(normAction, normEntity, normActor, pageable)
                .map(a -> new AuditLogResponse(
                        a.getId(),
                        a.getActorEmail(),
                        a.getActorRole(),
                        a.getAction(),
                        a.getEntityType(),
                        a.getEntityId(),
                        a.getOldValue(),
                        a.getNewValue(),
                        a.getDetails(),
                        a.getIpAddress(),
                        a.getCreatedAt()
                ));
    }

    private String normalize(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}