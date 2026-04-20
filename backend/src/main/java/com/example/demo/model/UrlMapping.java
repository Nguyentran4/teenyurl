package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class UrlMapping {
    private final long id;
    private final String shortCode;
    private final String originalUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private final AtomicLong clickCount;

    public UrlMapping(
        long id,
        String shortCode,
        String originalUrl,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
    ) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = new AtomicLong();
    }

    public long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public long getClickCount() {
        return clickCount.get();
    }

    public long incrementClickCount() {
        return clickCount.incrementAndGet();
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
