package com.example.ecommerce.dto;

import java.time.LocalDateTime;

public record AdminAlertEvent(
        String type,
        String title,
        String message,
        Long referenceId,
        LocalDateTime timestamp
) {}