package com.example.demo.service;

import com.example.demo.dto.CreateUrlRequest;
import com.example.demo.dto.CreateUrlResponse;
import com.example.demo.dto.UrlStatsResponse;
import com.example.demo.exception.InvalidUrlException;
import com.example.demo.exception.UrlExpiredException;
import com.example.demo.exception.UrlNotFoundException;
import com.example.demo.model.UrlMapping;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class UrlShortenerService {
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final AtomicLong idSequence = new AtomicLong(100_000);
    private final ConcurrentMap<String, UrlMapping> urlsByShortCode = new ConcurrentHashMap<>();
    private final Clock clock;

    public UrlShortenerService() {
        this(Clock.systemDefaultZone());
    }

    UrlShortenerService(Clock clock) {
        this.clock = clock;
    }

    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        String originalUrl = validateOriginalUrl(request);
        LocalDateTime now = LocalDateTime.now(clock);
        validateExpiration(request.expiresAt(), now);

        long id = idSequence.incrementAndGet();
        String shortCode = encodeBase62(id);
        UrlMapping mapping = new UrlMapping(id, shortCode, originalUrl, now, request.expiresAt());
        urlsByShortCode.put(shortCode, mapping);

        return new CreateUrlResponse(
            shortCode,
            buildShortUrl(shortCode),
            mapping.getOriginalUrl(),
            mapping.getCreatedAt(),
            mapping.getExpiresAt()
        );
    }

    public String resolveOriginalUrl(String shortCode) {
        UrlMapping mapping = findActiveMapping(shortCode);
        mapping.incrementClickCount();
        return mapping.getOriginalUrl();
    }

    public UrlStatsResponse getStats(String shortCode) {
        UrlMapping mapping = findActiveMapping(shortCode);
        return new UrlStatsResponse(
            mapping.getShortCode(),
            mapping.getOriginalUrl(),
            mapping.getClickCount(),
            mapping.getCreatedAt(),
            mapping.getExpiresAt()
        );
    }

    private UrlMapping findActiveMapping(String shortCode) {
        UrlMapping mapping = urlsByShortCode.get(shortCode);
        if (mapping == null) {
            throw new UrlNotFoundException(shortCode);
        }
        if (mapping.isExpired(LocalDateTime.now(clock))) {
            throw new UrlExpiredException(shortCode);
        }
        return mapping;
    }

    private String validateOriginalUrl(CreateUrlRequest request) {
        if (request == null || request.originalUrl() == null || request.originalUrl().isBlank()) {
            throw new InvalidUrlException("originalUrl is required");
        }

        String originalUrl = request.originalUrl().trim();
        try {
            URI uri = new URI(originalUrl);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                throw new InvalidUrlException("originalUrl must be an absolute HTTP or HTTPS URL");
            }

            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
                throw new InvalidUrlException("originalUrl must use HTTP or HTTPS");
            }
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException("originalUrl must be a valid URL");
        }

        return originalUrl;
    }

    private void validateExpiration(LocalDateTime expiresAt, LocalDateTime now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new InvalidUrlException("expiresAt must be in the future");
        }
    }

    private String buildShortUrl(String shortCode) {
        try {
            return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/")
                .path(shortCode)
                .toUriString();
        } catch (IllegalStateException exception) {
            return "http://localhost:8080/" + shortCode;
        }
    }

    private String encodeBase62(long value) {
        StringBuilder encoded = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            int index = (int) (remaining % BASE62.length());
            encoded.append(BASE62.charAt(index));
            remaining = remaining / BASE62.length();
        }
        return encoded.reverse().toString();
    }
}
