package com.example.demo.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
    String status,
    Instant checkedAt,
    Map<String, ComponentHealth> components
) {
    public boolean isHealthy() {
        return "UP".equals(status);
    }

    public record ComponentHealth(
        String status,
        String detail
    ) {
    }
}
