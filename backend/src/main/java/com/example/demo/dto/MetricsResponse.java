package com.example.demo.dto;

import java.time.Duration;
import java.time.Instant;

public record MetricsResponse(
    Instant startedAt,
    long uptimeSeconds,
    UrlMetrics urls,
    RuntimeMetrics runtime
) {
    public record UrlMetrics(
        long totalUrls,
        long totalClicks
    ) {
    }

    public record RuntimeMetrics(
        int availableProcessors,
        long usedMemoryBytes,
        long maxMemoryBytes
    ) {
    }

    public static long uptimeSeconds(Instant startedAt, Instant now) {
        return Duration.between(startedAt, now).toSeconds();
    }
}
