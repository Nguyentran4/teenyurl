package com.example.demo.dto;

public record ServiceStatusResponse(
    String service,
    String status,
    String message
) {
}
