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
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlShortenerService.class);

    private final UrlMappingRepository urlMappingRepository;
    private final UrlDailyClickRepository urlDailyClickRepository;
    private final RedirectCacheService redirectCacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final ShortCodeGenerator shortCodeGenerator;
    private final Clock clock;

    public UrlShortenerService(
        UrlMappingRepository urlMappingRepository,
        UrlDailyClickRepository urlDailyClickRepository,
        RedirectCacheService redirectCacheService,
        ApplicationEventPublisher eventPublisher,
        ShortCodeGenerator shortCodeGenerator,
        Clock clock
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.urlDailyClickRepository = urlDailyClickRepository;
        this.redirectCacheService = redirectCacheService;
        this.eventPublisher = eventPublisher;
        this.shortCodeGenerator = shortCodeGenerator;
        this.clock = clock;
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
            return urlMappingRepository.saveAndFlush(mapping);
        } catch (DataIntegrityViolationException exception) {
            throw new AliasAlreadyExistsException(shortCode);
        }
    }

    private UrlMapping saveGeneratedMapping(UrlMapping mapping) {
        String shortCode = shortCodeGenerator.nextShortCode();
        mapping.setShortCode(shortCode);

        try {
            return urlMappingRepository.saveAndFlush(mapping);
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

        String originalUrl = originalUrlValue.trim();
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

            validateUrlAuthority(uri);
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException("originalUrl must be a valid URL");
        } catch (IllegalArgumentException exception) {
            throw new InvalidUrlException("originalUrl must contain a valid host");
        }

        return originalUrl;
    }

    private void validateUrlAuthority(URI uri) {
        if (uri.getUserInfo() != null) {
            throw new InvalidUrlException("originalUrl must not include user info");
        }

        String host = uri.getHost();
        String asciiHost = IDN.toASCII(host);
        if (asciiHost.isBlank() || asciiHost.startsWith(".") || asciiHost.endsWith(".")) {
            throw new InvalidUrlException("originalUrl must contain a valid host");
        }

        int port = uri.getPort();
        if (port < -1 || port == 0 || port > 65535) {
            throw new InvalidUrlException("originalUrl must contain a valid port");
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
