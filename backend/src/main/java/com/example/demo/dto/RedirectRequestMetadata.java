package com.example.demo.dto;

public record RedirectRequestMetadata(
    String userAgent,
    String referrer,
    String clientIp
) {
    public static RedirectRequestMetadata empty() {
        return new RedirectRequestMetadata(null, null, null);
    }
}
