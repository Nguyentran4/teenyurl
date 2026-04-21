package com.example.demo.service;

import com.example.demo.model.UrlMapping;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "teenyurl.cache.redis.enabled", havingValue = "false")
public class NoOpRedirectCacheService implements RedirectCacheService {
    @Override
    public Optional<String> getOriginalUrl(String shortCode) {
        return Optional.empty();
    }

    @Override
    public void cacheRedirect(UrlMapping mapping, LocalDateTime now) {
    }

    @Override
    public void evict(String shortCode) {
    }
}
