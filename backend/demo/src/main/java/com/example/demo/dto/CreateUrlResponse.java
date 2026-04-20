package com.example.demo.dto;

import java.time.LocalDateTime;

public record CreateUrlResponse(
    String shortCode,
    String shortUrl,
    String originalUrl,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {
}
