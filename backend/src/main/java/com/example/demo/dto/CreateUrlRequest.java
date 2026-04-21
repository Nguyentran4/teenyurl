package com.example.demo.dto;

import java.time.LocalDateTime;

public record CreateUrlRequest(
    String originalUrl,
    String alias,
    LocalDateTime expiresAt
) {
}
