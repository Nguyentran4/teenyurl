package com.example.demo.config;

import com.example.demo.service.InMemoryRateLimiter;
import com.example.demo.service.RateLimiter;
import com.example.demo.service.RedisRateLimiter;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimiterConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Bean
    public RateLimiter rateLimiter(
        ObjectProvider<StringRedisTemplate> redisTemplateProvider,
        Clock clock,
        @Value("${teenyurl.rate-limit.redis.enabled:true}") boolean redisRateLimitEnabled,
        @Value("${teenyurl.rate-limit.redis.key-prefix:rate_limit:}") String redisKeyPrefix,
        @Value("${teenyurl.rate-limit.create.enabled:true}") boolean createLimitEnabled,
        @Value("${teenyurl.rate-limit.create.limit:10}") int createLimit,
        @Value("${teenyurl.rate-limit.create.window:PT1M}") Duration createWindow,
        @Value("${teenyurl.rate-limit.redirect.enabled:false}") boolean redirectLimitEnabled,
        @Value("${teenyurl.rate-limit.redirect.limit:120}") int redirectLimit,
        @Value("${teenyurl.rate-limit.redirect.window:PT1M}") Duration redirectWindow
    ) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisRateLimitEnabled && redisTemplate != null) {
            LOGGER.info("Using Redis-backed rate limiter");
            return new RedisRateLimiter(
                redisTemplate,
                redisKeyPrefix,
                createLimitEnabled,
                createLimit,
                createWindow,
                redirectLimitEnabled,
                redirectLimit,
                redirectWindow
            );
        }

        if (redisRateLimitEnabled) {
            LOGGER.warn("Redis rate limiting is enabled but Redis is unavailable; using in-memory rate limiter");
        } else {
            LOGGER.info("Redis rate limiting is disabled; using in-memory rate limiter");
        }

        return new InMemoryRateLimiter(
            clock,
            createLimitEnabled,
            createLimit,
            createWindow,
            redirectLimitEnabled,
            redirectLimit,
            redirectWindow
        );
    }
}
