package com.example.ecommerce.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesAnalyticsResponse(
    LocalDate date,
    Long ordersCount,
    BigDecimal revenue
) implements Serializable {}