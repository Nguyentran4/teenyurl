package com.example.demo;

import com.example.demo.model.UrlMapping;
import com.example.demo.service.RedirectCacheService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class SupportTestConfiguration {
    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-04-19T12:00:00Z"));
    }

    @Bean
    @Primary
    public InMemoryRedirectCacheService inMemoryRedirectCacheService() {
        return new InMemoryRedirectCacheService();
    }

    public static class InMemoryRedirectCacheService implements RedirectCacheService {
        private final Map<String, CacheEntry> cache = new HashMap<>();

        @Override
        public Optional<String> getOriginalUrl(String shortCode) {
            CacheEntry entry = cache.get(shortCode);
            if (entry == null) {
                return Optional.empty();
            }
            return Optional.of(entry.originalUrl());
        }

        @Override
        public void cacheRedirect(UrlMapping mapping, LocalDateTime now) {
            Duration ttl = mapping.getExpiresAt() == null
                ? Duration.ofHours(1)
                : Duration.between(now, mapping.getExpiresAt());
            if (ttl.isPositive()) {
                cache.put(mapping.getShortCode(), new CacheEntry(mapping.getOriginalUrl()));
            }
        }

        @Override
        public void evict(String shortCode) {
            cache.remove(shortCode);
        }

        public boolean contains(String shortCode) {
            return cache.containsKey(shortCode);
        }

        public void clear() {
            cache.clear();
        }

        private record CacheEntry(String originalUrl) {
        }
    }

    public static class MutableClock extends Clock {
        private Instant instant;

        public MutableClock(Instant instant) {
            this.instant = instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
