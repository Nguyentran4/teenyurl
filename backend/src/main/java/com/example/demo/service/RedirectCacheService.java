package com.example.demo.service;

import com.example.demo.model.UrlMapping;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RedirectCacheService {
    Optional<String> getOriginalUrl(String shortCode);

    void cacheRedirect(UrlMapping mapping, LocalDateTime now);

    void evict(String shortCode);
}
