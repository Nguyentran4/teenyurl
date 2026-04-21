package com.example.demo.service;

import com.example.demo.model.UrlMapping;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "teenyurl.cache.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisRedirectCacheService implements RedirectCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRedirectCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final Duration defaultTtl;

    public RedisRedirectCacheService(
        StringRedisTemplate redisTemplate,
        @Value("${teenyurl.cache.redirect.key-prefix:teenyurl:redirect:}") String keyPrefix,
        @Value("${teenyurl.cache.redirect.default-ttl:PT1H}") Duration defaultTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.defaultTtl = defaultTtl;
    }

    @Override
    public Optional<String> getOriginalUrl(String shortCode) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key(shortCode)));
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis redirect cache read failed for shortCode={}", shortCode, exception);
            return Optional.empty();
        }
    }

    @Override
    public void cacheRedirect(UrlMapping mapping, LocalDateTime now) {
        Duration ttl = ttlFor(mapping, now);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }

        try {
            redisTemplate
                .opsForValue()
                .set(key(mapping.getShortCode()), mapping.getOriginalUrl(), ttl);
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis redirect cache write failed for shortCode={}", mapping.getShortCode(), exception);
        }
    }

    @Override
    public void evict(String shortCode) {
        try {
            redisTemplate.delete(key(shortCode));
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis redirect cache eviction failed for shortCode={}", shortCode, exception);
        }
    }

    private Duration ttlFor(UrlMapping mapping, LocalDateTime now) {
        if (mapping.getExpiresAt() == null) {
            return defaultTtl;
        }
        return Duration.between(now, mapping.getExpiresAt());
    }

    private String key(String shortCode) {
        return keyPrefix + shortCode;
    }
}
