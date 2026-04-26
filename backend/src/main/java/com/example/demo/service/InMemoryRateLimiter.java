package com.example.demo.service;

import com.example.demo.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryRateLimiter implements RateLimiter {
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final Clock clock;
    private final boolean createLimitEnabled;
    private final int createLimit;
    private final Duration createWindow;
    private final boolean redirectLimitEnabled;
    private final int redirectLimit;
    private final Duration redirectWindow;

    public InMemoryRateLimiter(
        Clock clock,
        boolean createLimitEnabled,
        int createLimit,
        Duration createWindow,
        boolean redirectLimitEnabled,
        int redirectLimit,
        Duration redirectWindow
    ) {
        this.clock = clock;
        this.createLimitEnabled = createLimitEnabled;
        this.createLimit = createLimit;
        this.createWindow = createWindow;
        this.redirectLimitEnabled = redirectLimitEnabled;
        this.redirectLimit = redirectLimit;
        this.redirectWindow = redirectWindow;
    }

    @Override
    public void checkCreateAllowed(String clientIp) {
        if (createLimitEnabled) {
            checkAllowed("create", clientIp, createLimit, createWindow);
        }
    }

    @Override
    public void checkRedirectAllowed(String clientIp) {
        if (redirectLimitEnabled) {
            checkAllowed("redirect", clientIp, redirectLimit, redirectWindow);
        }
    }

    private void checkAllowed(String action, String clientIp, int limit, Duration window) {
        if (limit <= 0) {
            throw new RateLimitExceededException("Rate limit exceeded for " + action + " requests", window);
        }

        String normalizedIp = normalizeIp(clientIp);
        Instant now = clock.instant();
        String key = action + ":" + normalizedIp;
        WindowCounter counter = counters.compute(key, (ignored, current) -> nextCounter(current, now, window));

        int count = counter.count().incrementAndGet();
        if (count > limit) {
            throw new RateLimitExceededException(
                "Rate limit exceeded for " + action + " requests",
                retryAfter(counter.windowStartedAt(), now, window)
            );
        }
    }

    private WindowCounter nextCounter(WindowCounter current, Instant now, Duration window) {
        if (current == null || !current.windowStartedAt().plus(window).isAfter(now)) {
            return new WindowCounter(now, new AtomicInteger(0));
        }
        return current;
    }

    private Duration retryAfter(Instant windowStartedAt, Instant now, Duration window) {
        Duration retryAfter = Duration.between(now, windowStartedAt.plus(window));
        return retryAfter.isNegative() || retryAfter.isZero() ? Duration.ofSeconds(1) : retryAfter;
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim().toLowerCase();
    }

    private record WindowCounter(
        Instant windowStartedAt,
        AtomicInteger count
    ) {
    }
}
