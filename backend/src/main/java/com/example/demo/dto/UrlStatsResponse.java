package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UrlStatsResponse(
    String shortCode,
    String originalUrl,
    long clickCount,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    LocalDateTime lastAccessedAt,
    Analytics analytics
) {
    public record Analytics(
        long totalClicks,
        LocalDateTime lastAccessedAt,
        List<DailyClick> dailyClicks,
        LastRequest lastRequest
    ) {
    }

    public record DailyClick(
        LocalDate date,
        long count
    ) {
    }

    public record LastRequest(
        String userAgent,
        String referrer,
        String ipHash
    ) {
    }
}
