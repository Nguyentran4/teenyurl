package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.SupportTestConfiguration;
import com.example.demo.SupportTestConfiguration.InMemoryRedirectCacheService;
import com.example.demo.SupportTestConfiguration.MutableClock;
import com.example.demo.dto.CreateUrlRequest;
import com.example.demo.dto.CreateUrlResponse;
import com.example.demo.dto.UrlStatsResponse;
import com.example.demo.exception.InvalidUrlException;
import com.example.demo.exception.UrlExpiredException;
import com.example.demo.repository.UrlMappingRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(SupportTestConfiguration.class)
class UrlShortenerServiceTest {
    @Autowired
    private UrlShortenerService service;

    @Autowired
    private UrlMappingRepository repository;

    @Autowired
    private MutableClock clock;

    @Autowired
    private InMemoryRedirectCacheService redirectCacheService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        redirectCacheService.clear();
        clock.setInstant(Instant.parse("2026-04-19T12:00:00Z"));
    }

    @Test
    void createsShortUrlAndTracksClicks() {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/articles/123", null);

        CreateUrlResponse created = service.createShortUrl(request);
        String resolved = service.resolveOriginalUrl(created.shortCode());
        UrlStatsResponse stats = service.getStats(created.shortCode());

        assertThat(created.shortCode()).isNotBlank();
        assertThat(created.originalUrl()).isEqualTo("https://example.com/articles/123");
        assertThat(resolved).isEqualTo("https://example.com/articles/123");
        assertThat(stats.clickCount()).isEqualTo(1);
    }

    @Test
    void cachesRedirectLookupAndKeepsClickCountPersisted() {
        CreateUrlResponse created = service.createShortUrl(
            new CreateUrlRequest("https://example.com/cache-me", null)
        );

        assertThat(redirectCacheService.contains(created.shortCode())).isFalse();

        String firstRedirect = service.resolveOriginalUrl(created.shortCode());
        String secondRedirect = service.resolveOriginalUrl(created.shortCode());
        UrlStatsResponse stats = service.getStats(created.shortCode());

        assertThat(firstRedirect).isEqualTo("https://example.com/cache-me");
        assertThat(secondRedirect).isEqualTo("https://example.com/cache-me");
        assertThat(redirectCacheService.contains(created.shortCode())).isTrue();
        assertThat(stats.clickCount()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidUrl() {
        CreateUrlRequest request = new CreateUrlRequest("not-a-url", null);

        assertThatThrownBy(() -> service.createShortUrl(request))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("absolute HTTP or HTTPS URL");
    }

    @Test
    void rejectsPastExpiration() {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 4, 19, 11, 59);
        CreateUrlRequest request = new CreateUrlRequest("https://example.com", expiresAt);

        assertThatThrownBy(() -> service.createShortUrl(request))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("future");
    }

    @Test
    void blocksExpiredUrlOnRead() {
        CreateUrlResponse created = service.createShortUrl(
            new CreateUrlRequest("https://example.com", LocalDateTime.of(2026, 4, 19, 12, 1))
        );

        clock.setInstant(Instant.parse("2026-04-19T12:02:00Z"));

        assertThatThrownBy(() -> service.resolveOriginalUrl(created.shortCode()))
            .isInstanceOf(UrlExpiredException.class);
    }
}
