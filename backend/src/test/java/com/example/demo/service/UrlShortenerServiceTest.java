package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.SupportTestConfiguration;
import com.example.demo.SupportTestConfiguration.InMemoryRedirectCacheService;
import com.example.demo.SupportTestConfiguration.MutableClock;
import com.example.demo.dto.CreateUrlRequest;
import com.example.demo.dto.CreateUrlResponse;
import com.example.demo.dto.RedirectRequestMetadata;
import com.example.demo.dto.UrlStatsResponse;
import com.example.demo.exception.AliasAlreadyExistsException;
import com.example.demo.exception.InvalidUrlException;
import com.example.demo.exception.UrlExpiredException;
import com.example.demo.repository.UrlDailyClickRepository;
import com.example.demo.repository.UrlMappingRepository;
import java.time.Instant;
import java.time.LocalDate;
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
    private UrlDailyClickRepository dailyClickRepository;

    @Autowired
    private MutableClock clock;

    @Autowired
    private InMemoryRedirectCacheService redirectCacheService;

    @BeforeEach
    void setUp() {
        dailyClickRepository.deleteAll();
        repository.deleteAll();
        redirectCacheService.clear();
        clock.setInstant(Instant.parse("2026-04-19T12:00:00Z"));
    }

    @Test
    void createsShortUrlAndTracksClicks() {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/articles/123", null, null);

        CreateUrlResponse created = service.createShortUrl(request);
        String resolved = service.resolveOriginalUrl(created.shortCode());
        UrlStatsResponse stats = awaitStats(created.shortCode(), 1);

        assertThat(created.shortCode()).isNotBlank();
        assertThat(created.originalUrl()).isEqualTo("https://example.com/articles/123");
        assertThat(resolved).isEqualTo("https://example.com/articles/123");
        assertThat(stats.clickCount()).isEqualTo(1);
        assertThat(stats.lastAccessedAt()).isEqualTo(LocalDateTime.of(2026, 4, 19, 12, 0));
        assertThat(stats.analytics().totalClicks()).isEqualTo(1);
        assertThat(stats.analytics().dailyClicks())
            .containsExactly(new UrlStatsResponse.DailyClick(LocalDate.of(2026, 4, 19), 1));
    }

    @Test
    void cachesRedirectLookupAndKeepsClickCountPersisted() {
        CreateUrlResponse created = service.createShortUrl(
            new CreateUrlRequest("https://example.com/cache-me", null, null)
        );

        assertThat(redirectCacheService.contains(created.shortCode())).isFalse();

        String firstRedirect = service.resolveOriginalUrl(created.shortCode());
        String secondRedirect = service.resolveOriginalUrl(created.shortCode());
        UrlStatsResponse stats = awaitStats(created.shortCode(), 2);

        assertThat(firstRedirect).isEqualTo("https://example.com/cache-me");
        assertThat(secondRedirect).isEqualTo("https://example.com/cache-me");
        assertThat(redirectCacheService.contains(created.shortCode())).isTrue();
        assertThat(stats.clickCount()).isEqualTo(2);
        assertThat(stats.analytics().dailyClicks())
            .containsExactly(new UrlStatsResponse.DailyClick(LocalDate.of(2026, 4, 19), 2));
    }

    @Test
    void tracksDailyClicksLastAccessAndSafeRequestMetadata() {
        CreateUrlResponse created = service.createShortUrl(
            new CreateUrlRequest("https://example.com/analytics", null, null)
        );

        service.resolveOriginalUrl(created.shortCode(), new RedirectRequestMetadata(
            "JUnit Browser",
            "https://referrer.example/start",
            "203.0.113.10"
        ));
        clock.setInstant(Instant.parse("2026-04-20T09:15:00Z"));
        service.resolveOriginalUrl(created.shortCode(), new RedirectRequestMetadata(
            "JUnit Browser 2",
            null,
            "203.0.113.10"
        ));

        UrlStatsResponse stats = awaitStats(created.shortCode(), 2);

        assertThat(stats.clickCount()).isEqualTo(2);
        assertThat(stats.analytics().lastAccessedAt()).isEqualTo(LocalDateTime.of(2026, 4, 20, 9, 15));
        assertThat(stats.analytics().dailyClicks()).containsExactly(
            new UrlStatsResponse.DailyClick(LocalDate.of(2026, 4, 19), 1),
            new UrlStatsResponse.DailyClick(LocalDate.of(2026, 4, 20), 1)
        );
        assertThat(stats.analytics().lastRequest().userAgent()).isEqualTo("JUnit Browser 2");
        assertThat(stats.analytics().lastRequest().referrer()).isNull();
        assertThat(stats.analytics().lastRequest().ipHash())
            .hasSize(64)
            .doesNotContain("203.0.113.10");
    }

    @Test
    void rejectsInvalidUrl() {
        CreateUrlRequest request = new CreateUrlRequest("not-a-url", null, null);

        assertThatThrownBy(() -> service.createShortUrl(request))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("absolute HTTP or HTTPS URL");
    }

    @Test
    void rejectsPastExpiration() {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 4, 19, 11, 59);
        CreateUrlRequest request = new CreateUrlRequest("https://example.com", null, expiresAt);

        assertThatThrownBy(() -> service.createShortUrl(request))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("future");
    }

    @Test
    void blocksExpiredUrlOnRead() {
        CreateUrlResponse created = service.createShortUrl(
            new CreateUrlRequest("https://example.com", null, LocalDateTime.of(2026, 4, 19, 12, 1))
        );

        clock.setInstant(Instant.parse("2026-04-19T12:02:00Z"));

        assertThatThrownBy(() -> service.resolveOriginalUrl(created.shortCode()))
            .isInstanceOf(UrlExpiredException.class);
    }

    @Test
    void returnsStatsForExpiredUrl() {
        CreateUrlResponse created = service.createShortUrl(
            new CreateUrlRequest("https://example.com/expired-stats", null, LocalDateTime.of(2026, 4, 19, 12, 1))
        );

        clock.setInstant(Instant.parse("2026-04-19T12:02:00Z"));

        UrlStatsResponse stats = service.getStats(created.shortCode());

        assertThat(stats.originalUrl()).isEqualTo("https://example.com/expired-stats");
        assertThat(stats.expiresAt()).isEqualTo(LocalDateTime.of(2026, 4, 19, 12, 1));
    }

    @Test
    void createsShortUrlWithCustomAlias() {
        CreateUrlResponse created = service.createShortUrl(
            new CreateUrlRequest("https://example.com/custom", "my-alias_123", null)
        );

        String resolved = service.resolveOriginalUrl("my-alias_123");
        awaitStats("my-alias_123", 1);

        assertThat(created.shortCode()).isEqualTo("my-alias_123");
        assertThat(created.shortUrl()).endsWith("/my-alias_123");
        assertThat(resolved).isEqualTo("https://example.com/custom");
    }

    @Test
    void rejectsDuplicateAlias() {
        service.createShortUrl(new CreateUrlRequest("https://example.com/one", "taken", null));

        assertThatThrownBy(() -> service.createShortUrl(
                new CreateUrlRequest("https://example.com/two", "taken", null)
            ))
            .isInstanceOf(AliasAlreadyExistsException.class)
            .hasMessage("Alias is already taken: taken");
    }

    @Test
    void rejectsInvalidAliasCharacters() {
        assertThatThrownBy(() -> service.createShortUrl(
                new CreateUrlRequest("https://example.com", "bad alias!", null)
            ))
            .isInstanceOf(InvalidUrlException.class)
            .hasMessageContaining("alias must be 3-64 characters");
    }

    private UrlStatsResponse awaitStats(String shortCode, long expectedClicks) {
        long deadline = System.nanoTime() + 2_000_000_000L;
        UrlStatsResponse stats = service.getStats(shortCode);

        while (System.nanoTime() < deadline) {
            stats = service.getStats(shortCode);
            if (stats.clickCount() == expectedClicks) {
                return stats;
            }
            sleepBriefly();
        }

        return stats;
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(25);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for analytics update", exception);
        }
    }
}
