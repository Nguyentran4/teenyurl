package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.dto.CreateUrlRequest;
import com.example.demo.dto.CreateUrlResponse;
import com.example.demo.dto.UrlStatsResponse;
import com.example.demo.exception.InvalidUrlException;
import com.example.demo.exception.UrlExpiredException;
import com.example.demo.repository.UrlMappingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class UrlShortenerServiceTest {
    @Autowired
    private UrlShortenerService service;

    @Autowired
    private UrlMappingRepository repository;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
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

    @TestConfiguration
    static class ClockTestConfiguration {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2026-04-19T12:00:00Z"));
        }
    }

    static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
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
