package com.example.demo.exception;

import java.time.Duration;

public class RateLimitExceededException extends RuntimeException {
    private final Duration retryAfter;

    public RateLimitExceededException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
