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
    private InMemoryRedirectCacheService redirectCacheService;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
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

        mockMvc.perform(get("/" + shortCode))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com/very/long/link"));

        mockMvc.perform(get("/api/urls/" + shortCode + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value(shortCode))
            .andExpect(jsonPath("$.originalUrl").value("https://example.com/very/long/link"))
            .andExpect(jsonPath("$.clickCount").value(1));
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
            .andExpect(jsonPath("$.message").value("originalUrl must use HTTP or HTTPS"));
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
            .andExpect(jsonPath("$.message").value("alias must be 3-64 characters and contain only letters, numbers, hyphens, or underscores"));
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
            .andExpect(jsonPath("$.message").value("Alias is already taken: taken"));
    }

    @Test
    void returnsNotFoundForMissingShortCode() throws Exception {
        mockMvc.perform(get("/missing-code"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Short URL not found: missing-code"));
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
            .andExpect(jsonPath("$.message").value("Short URL has expired: " + shortCode))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());

        mockMvc.perform(get("/api/urls/" + shortCode + "/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value(shortCode))
            .andExpect(jsonPath("$.expiresAt").value("2026-04-19T12:01:00"));
    }
}
