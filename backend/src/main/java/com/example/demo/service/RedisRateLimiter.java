package com.example.demo.service;

import com.example.demo.exception.RateLimitExceededException;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisRateLimiter implements RateLimiter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final boolean createLimitEnabled;
    private final int createLimit;
    private final Duration createWindow;
    private final boolean redirectLimitEnabled;
    private final int redirectLimit;
    private final Duration redirectWindow;

    public RedisRateLimiter(
        StringRedisTemplate redisTemplate,
        String keyPrefix,
        boolean createLimitEnabled,
        int createLimit,
        Duration createWindow,
        boolean redirectLimitEnabled,
        int redirectLimit,
        Duration redirectWindow
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
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

        String key = key(action, clientIp);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                LOGGER.warn("Redis rate limiter increment returned null action={} key={}", action, key);
                return;
            }

            if (count == 1L) {
                redisTemplate.expire(key, window);
            }

            if (count > limit) {
                Long ttlSeconds = redisTemplate.getExpire(key);
                Duration retryAfter = ttlSeconds == null || ttlSeconds <= 0
                    ? Duration.ofSeconds(1)
                    : Duration.ofSeconds(ttlSeconds);
                throw new RateLimitExceededException("Rate limit exceeded for " + action + " requests", retryAfter);
            }
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis rate limiter failed action={} key={}", action, key, exception);
        }
    }

    private String key(String action, String clientIp) {
        return keyPrefix + action + ":" + normalizeIp(clientIp);
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim().toLowerCase(Locale.ROOT);
    }
}
