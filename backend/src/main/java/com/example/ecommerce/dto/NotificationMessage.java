package com.example.ecommerce.dto;

import java.io.Serializable;

public record NotificationMessage(
    String toEmail,
    String subject,
    String body
) implements Serializable {}