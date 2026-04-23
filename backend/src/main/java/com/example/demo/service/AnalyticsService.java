package com.example.demo.service;

import com.example.demo.dto.RedirectRequestMetadata;
import com.example.demo.model.UrlDailyClick;
import com.example.demo.model.UrlMapping;
import com.example.demo.repository.UrlDailyClickRepository;
import com.example.demo.repository.UrlMappingRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final UrlMappingRepository urlMappingRepository;
    private final UrlDailyClickRepository urlDailyClickRepository;
    private final String ipHashSalt;

    public AnalyticsService(
        UrlMappingRepository urlMappingRepository,
        UrlDailyClickRepository urlDailyClickRepository,
        @Value("${teenyurl.analytics.ip-hash-salt:teenyurl-local-dev}") String ipHashSalt
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.urlDailyClickRepository = urlDailyClickRepository;
        this.ipHashSalt = ipHashSalt;
    }

    @Async("analyticsTaskExecutor")
    @EventListener
    @Transactional
    public void recordAccess(UrlAccessedEvent event) {
        AccessMetadata accessMetadata = sanitizeMetadata(event.metadata());
        int updatedRows = urlMappingRepository.recordAccessForRedirect(
            event.shortCode(),
            event.accessedAt(),
            event.accessedAt(),
            accessMetadata.userAgent(),
            accessMetadata.referrer(),
            accessMetadata.ipHash()
        );

        if (updatedRows != 1) {
            return;
        }

        urlMappingRepository
            .findByShortCode(event.shortCode())
            .ifPresent(mapping -> recordDailyClick(mapping, event.accessedAt().toLocalDate()));
    }

    private void recordDailyClick(UrlMapping mapping, LocalDate accessDate) {
        UrlDailyClick dailyClick = urlDailyClickRepository
            .findByUrlMappingAndAccessDate(mapping, accessDate)
            .orElseGet(() -> new UrlDailyClick(mapping, accessDate));
        dailyClick.incrementClickCount();
        urlDailyClickRepository.save(dailyClick);
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
