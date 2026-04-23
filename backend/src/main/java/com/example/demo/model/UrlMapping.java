package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "url_mappings")
public class UrlMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "url_mapping_id_generator")
    @SequenceGenerator(name = "url_mapping_id_generator", sequenceName = "url_mapping_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "short_code", unique = true, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "last_user_agent", length = 512)
    private String lastUserAgent;

    @Column(name = "last_referrer", length = 2048)
    private String lastReferrer;

    @Column(name = "last_ip_hash", length = 64)
    private String lastIpHash;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected UrlMapping() {
    }

    public UrlMapping(
        String originalUrl,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
    ) {
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = 0;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
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
        return clickCount;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public String getLastUserAgent() {
        return lastUserAgent;
    }

    public String getLastReferrer() {
        return lastReferrer;
    }

    public String getLastIpHash() {
        return lastIpHash;
    }

    public void recordAccess(LocalDateTime accessedAt, String userAgent, String referrer, String ipHash) {
        clickCount++;
        lastAccessedAt = accessedAt;
        lastUserAgent = trimToLength(userAgent, 512);
        lastReferrer = trimToLength(referrer, 2048);
        lastIpHash = ipHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
