package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.service.InMemoryRateLimiter;
import com.example.demo.service.RateLimiter;
import com.example.demo.service.RedisRateLimiter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

class RateLimiterContextTest {
    @Nested
    @SpringBootTest(properties = {
        "teenyurl.cache.redis.enabled=false",
        "teenyurl.rate-limit.redis.enabled=true"
    })
    class RedisAvailableContext {
        @Autowired
        private RateLimiter rateLimiter;

        @Test
        void usesRedisRateLimiterWhenRedisIsEnabledAndAvailable() {
            assertThat(rateLimiter).isInstanceOf(RedisRateLimiter.class);
        }
    }

    @Nested
    @SpringBootTest(properties = {
        "teenyurl.cache.redis.enabled=false",
        "teenyurl.rate-limit.redis.enabled=true",
        "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration"
    })
    class RedisUnavailableContext {
        @Autowired
        private RateLimiter rateLimiter;

        @Test
        void fallsBackToInMemoryRateLimiterWhenRedisIsUnavailable() {
            assertThat(rateLimiter).isInstanceOf(InMemoryRateLimiter.class);
        }
    }
}
