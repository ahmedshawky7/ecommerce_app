package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Role;

import java.io.Serializable;
import java.time.LocalDateTime;

public record UserManagementResponse(
    Long id,
    String username,
    String email,
    Role role,
    boolean isEnabled,
    LocalDateTime createdAt
) implements Serializable {}