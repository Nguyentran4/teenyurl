package com.example.demo.service;

import com.example.demo.dto.CreateUrlRequest;
import com.example.demo.dto.CreateUrlResponse;
import com.example.demo.dto.RedirectRequestMetadata;
import com.example.demo.dto.UrlStatsResponse;
import com.example.demo.exception.AliasAlreadyExistsException;
import com.example.demo.exception.InvalidUrlException;
import com.example.demo.exception.UrlExpiredException;
import com.example.demo.exception.UrlNotFoundException;
import com.example.demo.model.UrlDailyClick;
import com.example.demo.model.UrlMapping;
import com.example.demo.repository.UrlDailyClickRepository;
import com.example.demo.repository.UrlMappingRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class UrlShortenerService {
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");

    private final UrlMappingRepository urlMappingRepository;
    private final UrlDailyClickRepository urlDailyClickRepository;
    private final RedirectCacheService redirectCacheService;
    private final Clock clock;
    private final String ipHashSalt;

    public UrlShortenerService(
        UrlMappingRepository urlMappingRepository,
        UrlDailyClickRepository urlDailyClickRepository,
        RedirectCacheService redirectCacheService,
        Clock clock,
        @Value("${teenyurl.analytics.ip-hash-salt:teenyurl-local-dev}") String ipHashSalt
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.urlDailyClickRepository = urlDailyClickRepository;
        this.redirectCacheService = redirectCacheService;
        this.clock = clock;
        this.ipHashSalt = ipHashSalt;
    }

    @Transactional
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        String originalUrl = validateOriginalUrl(request);
        LocalDateTime now = LocalDateTime.now(clock);
        validateExpiration(request.expiresAt(), now);
        String alias = validateAlias(request.alias());

        UrlMapping mapping = new UrlMapping(originalUrl, now, request.expiresAt());
        String shortCode;

        if (alias == null) {
            mapping = urlMappingRepository.saveAndFlush(mapping);
            shortCode = encodeBase62(mapping.getId());
            mapping.setShortCode(shortCode);
            mapping = saveMapping(mapping, shortCode);
        } else {
            shortCode = alias;
            ensureAliasAvailable(shortCode);
            mapping.setShortCode(shortCode);
            mapping = saveMapping(mapping, shortCode);
        }

        return new CreateUrlResponse(
            shortCode,
            buildShortUrl(shortCode),
            mapping.getOriginalUrl(),
            mapping.getCreatedAt(),
            mapping.getExpiresAt()
        );
    }

    private UrlMapping saveMapping(UrlMapping mapping, String shortCode) {
        try {
            return urlMappingRepository.saveAndFlush(mapping);
        } catch (DataIntegrityViolationException exception) {
            throw new AliasAlreadyExistsException(shortCode);
        }
    }

    @Transactional
    public String resolveOriginalUrl(String shortCode) {
        return resolveOriginalUrl(shortCode, RedirectRequestMetadata.empty());
    }

    @Transactional
    public String resolveOriginalUrl(String shortCode, RedirectRequestMetadata metadata) {
        LocalDateTime now = LocalDateTime.now(clock);
        AccessMetadata accessMetadata = sanitizeMetadata(metadata);
        return redirectCacheService
            .getOriginalUrl(shortCode)
            .map(originalUrl -> resolveCachedRedirect(shortCode, originalUrl, now, accessMetadata))
            .orElseGet(() -> resolveDatabaseRedirect(shortCode, now, accessMetadata));
    }

    private String resolveCachedRedirect(
        String shortCode,
        String originalUrl,
        LocalDateTime now,
        AccessMetadata accessMetadata
    ) {
        int updatedRows = urlMappingRepository.recordAccessForRedirect(
            shortCode,
            now,
            now,
            accessMetadata.userAgent(),
            accessMetadata.referrer(),
            accessMetadata.ipHash()
        );
        if (updatedRows == 1) {
            recordDailyClick(shortCode, now.toLocalDate());
            return originalUrl;
        }

        redirectCacheService.evict(shortCode);
        UrlMapping mapping = findActiveMapping(shortCode, now);
        mapping.recordAccess(now, accessMetadata.userAgent(), accessMetadata.referrer(), accessMetadata.ipHash());
        recordDailyClick(mapping, now.toLocalDate());
        redirectCacheService.cacheRedirect(mapping, now);
        return mapping.getOriginalUrl();
    }

    private String resolveDatabaseRedirect(String shortCode, LocalDateTime now, AccessMetadata accessMetadata) {
        UrlMapping mapping = findActiveMapping(shortCode, now);
        mapping.recordAccess(now, accessMetadata.userAgent(), accessMetadata.referrer(), accessMetadata.ipHash());
        recordDailyClick(mapping, now.toLocalDate());
        redirectCacheService.cacheRedirect(mapping, now);
        return mapping.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        UrlMapping mapping = findExistingMapping(shortCode);
        return new UrlStatsResponse(
            mapping.getShortCode(),
            mapping.getOriginalUrl(),
            mapping.getClickCount(),
            mapping.getCreatedAt(),
            mapping.getExpiresAt(),
            mapping.getLastAccessedAt(),
            buildAnalytics(mapping)
        );
    }

    private UrlStatsResponse.Analytics buildAnalytics(UrlMapping mapping) {
        List<UrlStatsResponse.DailyClick> dailyClicks = urlDailyClickRepository
            .findByUrlMappingOrderByAccessDateAsc(mapping)
            .stream()
            .map(dailyClick -> new UrlStatsResponse.DailyClick(
                dailyClick.getAccessDate(),
                dailyClick.getClickCount()
            ))
            .toList();

        UrlStatsResponse.LastRequest lastRequest = new UrlStatsResponse.LastRequest(
            mapping.getLastUserAgent(),
            mapping.getLastReferrer(),
            mapping.getLastIpHash()
        );

        return new UrlStatsResponse.Analytics(
            mapping.getClickCount(),
            mapping.getLastAccessedAt(),
            dailyClicks,
            lastRequest
        );
    }

    private void recordDailyClick(String shortCode, LocalDate accessDate) {
        UrlMapping mapping = findExistingMapping(shortCode);
        recordDailyClick(mapping, accessDate);
    }

    private void recordDailyClick(UrlMapping mapping, LocalDate accessDate) {
        UrlDailyClick dailyClick = urlDailyClickRepository
            .findByUrlMappingAndAccessDate(mapping, accessDate)
            .orElseGet(() -> new UrlDailyClick(mapping, accessDate));
        dailyClick.incrementClickCount();
        urlDailyClickRepository.save(dailyClick);
    }

    private UrlMapping findActiveMapping(String shortCode) {
        return findActiveMapping(shortCode, LocalDateTime.now(clock));
    }

    private UrlMapping findExistingMapping(String shortCode) {
        UrlMapping mapping = urlMappingRepository
            .findByShortCode(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));
        if (!mapping.isActive()) {
            throw new UrlNotFoundException(shortCode);
        }
        return mapping;
    }

    private UrlMapping findActiveMapping(String shortCode, LocalDateTime now) {
        UrlMapping mapping = findExistingMapping(shortCode);
        if (mapping.isExpired(now)) {
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

    private String validateAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }

        String normalizedAlias = alias.trim();
        if (!ALIAS_PATTERN.matcher(normalizedAlias).matches()) {
            throw new InvalidUrlException("alias must be 3-64 characters and contain only letters, numbers, hyphens, or underscores");
        }

        return normalizedAlias;
    }

    private void ensureAliasAvailable(String alias) {
        if (urlMappingRepository.existsByShortCode(alias)) {
            throw new AliasAlreadyExistsException(alias);
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

    private String encodeBase62(Long value) {
        StringBuilder encoded = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            int index = (int) (remaining % BASE62.length());
            encoded.append(BASE62.charAt(index));
            remaining = remaining / BASE62.length();
        }
        return encoded.reverse().toString();
    }

    private AccessMetadata sanitizeMetadata(RedirectRequestMetadata metadata) {
        RedirectRequestMetadata safeMetadata = metadata == null ? RedirectRequestMetadata.empty() : metadata;
        return new AccessMetadata(
            trimToLength(safeMetadata.userAgent(), 512),
            trimToLength(safeMetadata.referrer(), 2048),
            hashIpAddress(safeMetadata.clientIp())
        );
    }

    private String hashIpAddress(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }

        String normalizedIp = clientIp.trim().toLowerCase(Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((ipHashSalt + ":" + normalizedIp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private record AccessMetadata(
        String userAgent,
        String referrer,
        String ipHash
    ) {
    }
}
