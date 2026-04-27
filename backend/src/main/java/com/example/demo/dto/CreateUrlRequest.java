package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateUrlRequest(
    @NotBlank(message = "is required")
    @Size(max = 2048, message = "must be 2048 characters or fewer")
    String originalUrl,

    @Size(min = 3, max = 64, message = "must be 3-64 characters and contain only letters, numbers, hyphens, or underscores")
    @Pattern(
        regexp = "^[A-Za-z0-9_-]+$",
        message = "must be 3-64 characters and contain only letters, numbers, hyphens, or underscores"
    )
    @JsonAlias("customAlias")
    String alias,

    LocalDateTime expiresAt
) {
}
