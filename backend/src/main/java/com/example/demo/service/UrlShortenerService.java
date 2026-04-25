package com.example.demo.service;

import com.example.demo.dto.CreateUrlRequest;
import com.example.demo.dto.CreateUrlResponse;
import com.example.demo.dto.RedirectRequestMetadata;
import com.example.demo.dto.UrlStatsResponse;
import com.example.demo.exception.AliasAlreadyExistsException;
import com.example.demo.exception.InvalidUrlException;
import com.example.demo.exception.UrlExpiredException;
import com.example.demo.exception.UrlNotFoundException;
import com.example.demo.model.UrlMapping;
import com.example.demo.repository.UrlDailyClickRepository;
import com.example.demo.repository.UrlMappingRepository;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class UrlShortenerService {
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");
    private static final Pattern DISALLOWED_URL_CHARACTERS = Pattern.compile(".*[\\p{Cntrl}\\s].*");
    private static final Pattern IPV4_ADDRESS = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlShortenerService.class);

    private final UrlMappingRepository urlMappingRepository;
    private final UrlDailyClickRepository urlDailyClickRepository;
    private final RedirectCacheService redirectCacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final ShortCodeGenerator shortCodeGenerator;
    private final Clock clock;
    private final boolean allowPrivateRedirectTargets;

    public UrlShortenerService(
        UrlMappingRepository urlMappingRepository,
        UrlDailyClickRepository urlDailyClickRepository,
        RedirectCacheService redirectCacheService,
        ApplicationEventPublisher eventPublisher,
        ShortCodeGenerator shortCodeGenerator,
        Clock clock,
        @Value("${teenyurl.security.allow-private-redirect-targets:false}") boolean allowPrivateRedirectTargets
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.urlDailyClickRepository = urlDailyClickRepository;
        this.redirectCacheService = redirectCacheService;
        this.eventPublisher = eventPublisher;
        this.shortCodeGenerator = shortCodeGenerator;
        this.clock = clock;
        this.allowPrivateRedirectTargets = allowPrivateRedirectTargets;
    }

    @Transactional
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        if (request == null) {
            throw new InvalidUrlException("request body is required");
        }

        String originalUrl = validateOriginalUrl(request.originalUrl());
        LocalDateTime now = LocalDateTime.now(clock);
        validateExpiration(request.expiresAt(), now);
        String alias = validateAlias(request.alias());

        UrlMapping mapping = new UrlMapping(originalUrl, now, request.expiresAt());
        String shortCode;

        if (alias == null) {
            mapping = saveGeneratedMapping(mapping);
            shortCode = mapping.getShortCode();
        } else {
            shortCode = alias;
            ensureAliasAvailable(shortCode);
            mapping.setShortCode(shortCode);
            mapping = saveMapping(mapping, shortCode);
        }

        LOGGER.info(
            "Created short URL shortCode={} originalUrl={} customAlias={} expiresAt={}",
            shortCode,
            mapping.getOriginalUrl(),
            alias != null,
            mapping.getExpiresAt()
        );

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
            UrlMapping savedMapping = urlMappingRepository.saveAndFlush(mapping);
            redirectCacheService.evict(shortCode);
            return savedMapping;
        } catch (DataIntegrityViolationException exception) {
            throw new AliasAlreadyExistsException(shortCode);
        }
    }

    private UrlMapping saveGeneratedMapping(UrlMapping mapping) {
        String shortCode = shortCodeGenerator.nextShortCode();
        mapping.setShortCode(shortCode);

        try {
            UrlMapping savedMapping = urlMappingRepository.saveAndFlush(mapping);
            redirectCacheService.evict(shortCode);
            return savedMapping;
        } catch (DataIntegrityViolationException exception) {
            LOGGER.error(
                "Generated short code collision shortCode={} likelyMisconfiguredNodeId=true",
                shortCode,
                exception
            );
            throw new IllegalStateException("Generated short code collision detected", exception);
        }
    }

    @Transactional(readOnly = true)
    public String resolveOriginalUrl(String shortCode) {
        return resolveOriginalUrl(shortCode, RedirectRequestMetadata.empty());
    }

    @Transactional(readOnly = true)
    public String resolveOriginalUrl(String shortCode, RedirectRequestMetadata metadata) {
        LocalDateTime now = LocalDateTime.now(clock);
        return redirectCacheService
            .getOriginalUrl(shortCode)
            .map(originalUrl -> resolveCachedRedirect(shortCode, originalUrl, now, metadata))
            .orElseGet(() -> resolveDatabaseRedirect(shortCode, now, metadata));
    }

    private String resolveCachedRedirect(
        String shortCode,
        String originalUrl,
        LocalDateTime now,
        RedirectRequestMetadata metadata
    ) {
        eventPublisher.publishEvent(new UrlAccessedEvent(shortCode, now, metadata));
        LOGGER.info("Resolved redirect from cache shortCode={}", shortCode);
        return originalUrl;
    }

    private String resolveDatabaseRedirect(String shortCode, LocalDateTime now, RedirectRequestMetadata metadata) {
        UrlMapping mapping = findActiveMapping(shortCode, now);
        redirectCacheService.cacheRedirect(mapping, now);
        eventPublisher.publishEvent(new UrlAccessedEvent(shortCode, now, metadata));
        LOGGER.info("Resolved redirect from database shortCode={}", shortCode);
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

    private String validateOriginalUrl(String originalUrlValue) {
        if (originalUrlValue == null || originalUrlValue.isBlank()) {
            throw new InvalidUrlException("originalUrl is required");
        }

        String originalUrl = originalUrlValue.strip();
        if (originalUrl.length() > 2048) {
            throw new InvalidUrlException("originalUrl must be 2048 characters or fewer");
        }

        if (DISALLOWED_URL_CHARACTERS.matcher(originalUrl).matches()) {
            throw new InvalidUrlException("originalUrl must not contain whitespace or control characters");
        }

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

            String sanitizedUrl = sanitizeUrl(uri, normalizedScheme);
            if (sanitizedUrl.length() > 2048) {
                throw new InvalidUrlException("originalUrl must be 2048 characters or fewer");
            }
            return sanitizedUrl;
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException("originalUrl must be a valid URL");
        } catch (IllegalArgumentException exception) {
            throw new InvalidUrlException("originalUrl must contain a valid host");
        }
    }

    private String sanitizeUrl(URI uri, String normalizedScheme) throws URISyntaxException {
        if (uri.getUserInfo() != null) {
            throw new InvalidUrlException("originalUrl must not include user info");
        }

        String host = uri.getHost();
        String asciiHost = normalizeHost(host);
        if (asciiHost.isBlank() || asciiHost.startsWith(".") || asciiHost.endsWith(".")) {
            throw new InvalidUrlException("originalUrl must contain a valid host");
        }

        validateRedirectTargetHost(asciiHost);

        int port = uri.getPort();
        if (port < -1 || port == 0 || port > 65535) {
            throw new InvalidUrlException("originalUrl must contain a valid port");
        }

        return rebuildUrl(uri, normalizedScheme, asciiHost);
    }

    private String normalizeHost(String host) {
        String normalizedHost = host.replace("[", "").replace("]", "");
        if (normalizedHost.contains(":")) {
            return normalizedHost.toLowerCase(Locale.ROOT);
        }
        return IDN.toASCII(normalizedHost).toLowerCase(Locale.ROOT);
    }

    private void validateRedirectTargetHost(String asciiHost) {
        if (allowPrivateRedirectTargets) {
            return;
        }

        String host = asciiHost.toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.endsWith(".localhost")) {
            throw new InvalidUrlException("originalUrl must not target localhost");
        }

        if (isPrivateIpv4Address(host) || isPrivateIpv6Address(host)) {
            throw new InvalidUrlException("originalUrl must not target private or local network addresses");
        }
    }

    private boolean isPrivateIpv4Address(String host) {
        if (!IPV4_ADDRESS.matcher(host).matches()) {
            return false;
        }

        String[] parts = host.split("\\.");
        int first = parseIpv4Part(parts[0]);
        int second = parseIpv4Part(parts[1]);
        int third = parseIpv4Part(parts[2]);
        int fourth = parseIpv4Part(parts[3]);
        if (first < 0 || second < 0 || third < 0 || fourth < 0) {
            throw new InvalidUrlException("originalUrl must contain a valid host");
        }

        return first == 0
            || first == 10
            || first == 127
            || (first == 100 && second >= 64 && second <= 127)
            || (first == 169 && second == 254)
            || (first == 172 && second >= 16 && second <= 31)
            || (first == 192 && second == 168);
    }

    private int parseIpv4Part(String value) {
        int number = Integer.parseInt(value);
        return number >= 0 && number <= 255 ? number : -1;
    }

    private boolean isPrivateIpv6Address(String host) {
        String normalizedHost = host.replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
        if (!normalizedHost.contains(":")) {
            return false;
        }

        return normalizedHost.equals("::1")
            || normalizedHost.startsWith("fe80:")
            || normalizedHost.startsWith("fc")
            || normalizedHost.startsWith("fd");
    }

    private String rebuildUrl(URI uri, String scheme, String host) {
        StringBuilder sanitized = new StringBuilder();
        sanitized.append(scheme).append("://").append(formatHost(host));
        if (uri.getPort() != -1) {
            sanitized.append(":").append(uri.getPort());
        }
        appendIfPresent(sanitized, uri.getRawPath());
        appendPrefixedIfPresent(sanitized, "?", uri.getRawQuery());
        appendPrefixedIfPresent(sanitized, "#", uri.getRawFragment());
        return sanitized.toString();
    }

    private String formatHost(String host) {
        return host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (value != null) {
            builder.append(value);
        }
    }

    private void appendPrefixedIfPresent(StringBuilder builder, String prefix, String value) {
        if (value != null) {
            builder.append(prefix).append(value);
        }
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
}
