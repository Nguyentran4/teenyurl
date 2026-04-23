package com.example.demo.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.SupportTestConfiguration;
import com.example.demo.SupportTestConfiguration.InMemoryRedirectCacheService;
import com.example.demo.SupportTestConfiguration.MutableClock;
import com.example.demo.repository.UrlDailyClickRepository;
import com.example.demo.repository.UrlMappingRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SupportTestConfiguration.class)
class UrlControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlMappingRepository repository;

    @Autowired
    private UrlDailyClickRepository dailyClickRepository;

    @Autowired
    private InMemoryRedirectCacheService redirectCacheService;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        dailyClickRepository.deleteAll();
        repository.deleteAll();
        redirectCacheService.clear();
        clock.setInstant(Instant.parse("2026-04-19T12:00:00Z"));
    }

    @Test
    void createsRedirectsAndReturnsStats() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "https://example.com/very/long/link"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty())
            .andExpect(jsonPath("$.shortUrl", startsWith("http://localhost/")))
            .andExpect(jsonPath("$.originalUrl").value("https://example.com/very/long/link"))
            .andReturn();

        JsonNode response = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String shortCode = response.get("shortCode").asText();

        mockMvc.perform(get("/" + shortCode)
                .header("User-Agent", "MockMvc Browser")
                .header("Referer", "https://referrer.example/home")
                .header("X-Forwarded-For", "198.51.100.22, 10.0.0.1"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com/very/long/link"));

        awaitClickCount(shortCode, 1);

        mockMvc.perform(get("/api/urls/" + shortCode + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value(shortCode))
            .andExpect(jsonPath("$.originalUrl").value("https://example.com/very/long/link"))
            .andExpect(jsonPath("$.clickCount").value(1))
            .andExpect(jsonPath("$.lastAccessedAt").value("2026-04-19T12:00:00"))
            .andExpect(jsonPath("$.analytics.totalClicks").value(1))
            .andExpect(jsonPath("$.analytics.lastAccessedAt").value("2026-04-19T12:00:00"))
            .andExpect(jsonPath("$.analytics.dailyClicks[0].date").value("2026-04-19"))
            .andExpect(jsonPath("$.analytics.dailyClicks[0].count").value(1))
            .andExpect(jsonPath("$.analytics.lastRequest.userAgent").value("MockMvc Browser"))
            .andExpect(jsonPath("$.analytics.lastRequest.referrer").value("https://referrer.example/home"))
            .andExpect(jsonPath("$.analytics.lastRequest.ipHash").isNotEmpty());
    }

    @Test
    void createsUrlWithCustomAlias() throws Exception {
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "https://example.com/custom-alias",
                      "alias": "launch_2026"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").value("launch_2026"))
            .andExpect(jsonPath("$.shortUrl", startsWith("http://localhost/launch_2026")))
            .andExpect(jsonPath("$.originalUrl").value("https://example.com/custom-alias"));

        mockMvc.perform(get("/launch_2026"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com/custom-alias"));

        awaitClickCount("launch_2026", 1);
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "ftp://example.com/file"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("originalUrl must use HTTP or HTTPS"))
            .andExpect(jsonPath("$.path").value("/api/urls"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void rejectsInvalidAlias() throws Exception {
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "https://example.com",
                      "alias": "bad alias!"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("alias must be 3-64 characters and contain only letters, numbers, hyphens, or underscores"))
            .andExpect(jsonPath("$.path").value("/api/urls"));
    }

    @Test
    void rejectsMissingOriginalUrlWithConsistentErrorBody() throws Exception {
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "alias": "missing_url"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("originalUrl is required"))
            .andExpect(jsonPath("$.path").value("/api/urls"));
    }

    @Test
    void rejectsDuplicateAlias() throws Exception {
        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "https://example.com/first",
                      "alias": "taken"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "https://example.com/second",
                      "alias": "taken"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Alias is already taken: taken"))
            .andExpect(jsonPath("$.path").value("/api/urls"));
    }

    @Test
    void returnsNotFoundForMissingShortCode() throws Exception {
        mockMvc.perform(get("/missing-code"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Short URL not found: missing-code"))
            .andExpect(jsonPath("$.path").value("/missing-code"));
    }

    @Test
    void createsUrlWithExpirationAndReturnsItInStats() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "https://example.com/with-expiration",
                      "expiresAt": "2026-04-19T12:05:00"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty())
            .andExpect(jsonPath("$.originalUrl").value("https://example.com/with-expiration"))
            .andExpect(jsonPath("$.expiresAt").value("2026-04-19T12:05:00"))
            .andReturn();

        JsonNode response = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String shortCode = response.get("shortCode").asText();

        mockMvc.perform(get("/api/urls/" + shortCode + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value(shortCode))
            .andExpect(jsonPath("$.originalUrl").value("https://example.com/with-expiration"))
            .andExpect(jsonPath("$.expiresAt").value("2026-04-19T12:05:00"));
    }

    @Test
    void expiredUrlDoesNotRedirectAndReturnsCleanError() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "https://example.com/expires-soon",
                      "expiresAt": "2026-04-19T12:01:00"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode response = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String shortCode = response.get("shortCode").asText();

        clock.setInstant(Instant.parse("2026-04-19T12:02:00Z"));

        mockMvc.perform(get("/" + shortCode))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.status").value(410))
            .andExpect(jsonPath("$.error").value("Gone"))
            .andExpect(jsonPath("$.message").value("Short URL has expired: " + shortCode))
            .andExpect(jsonPath("$.path").value("/" + shortCode))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());

        mockMvc.perform(get("/api/urls/" + shortCode + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value(shortCode))
            .andExpect(jsonPath("$.expiresAt").value("2026-04-19T12:01:00"));
    }

    private void awaitClickCount(String shortCode, long expectedClickCount) {
        long deadline = System.nanoTime() + 2_000_000_000L;

        while (System.nanoTime() < deadline) {
            long clickCount = repository
                .findByShortCode(shortCode)
                .map(mapping -> mapping.getClickCount())
                .orElse(0L);
            if (clickCount == expectedClickCount) {
                return;
            }
            sleepBriefly();
        }
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
