package com.example.ecommerce.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @Size(max = 150)
        String title,

        @NotBlank(message = "Comment is required")
        @Size(min = 5, max = 2000, message = "Comment must be between 5 and 2000 characters")
        String comment
) {}