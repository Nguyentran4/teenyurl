package com.example.demo.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    String message,
    LocalDateTime timestamp
) {
}
