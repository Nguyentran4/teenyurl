package com.example.demo.service;

import com.example.demo.dto.RedirectRequestMetadata;
import java.time.LocalDateTime;

public record UrlAccessedEvent(
    String shortCode,
    LocalDateTime accessedAt,
    RedirectRequestMetadata metadata
) {
}
