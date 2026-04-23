package com.example.demo.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
    "teenyurl.rate-limit.create.limit=2",
    "teenyurl.rate-limit.create.window=PT1M",
    "teenyurl.rate-limit.redirect.enabled=true",
    "teenyurl.rate-limit.redirect.limit=2",
    "teenyurl.rate-limit.redirect.window=PT1M"
})
@AutoConfigureMockMvc
@Import(SupportTestConfiguration.class)
class RateLimitIntegrationTest {
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
    void returnsTooManyRequestsWhenCreateLimitIsExceeded() throws Exception {
        String clientIp = "203.0.113.210";

        createUrl(clientIp, "https://example.com/one")
            .andExpect(status().isCreated());
        createUrl(clientIp, "https://example.com/two")
            .andExpect(status().isCreated());

        createUrl(clientIp, "https://example.com/three")
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.error").value("Too Many Requests"))
            .andExpect(jsonPath("$.message").value("Rate limit exceeded for create requests"))
            .andExpect(jsonPath("$.path").value("/api/urls"));
    }

    @Test
    void optionallyLimitsRedirectsPerIp() throws Exception {
        String shortCode = createShortCode("198.51.100.10", "https://example.com/redirect-limit");
        String redirectIp = "203.0.113.220";

        mockMvc.perform(get("/" + shortCode).header("X-Forwarded-For", redirectIp))
            .andExpect(status().isFound());
        mockMvc.perform(get("/" + shortCode).header("X-Forwarded-For", redirectIp))
            .andExpect(status().isFound());

        mockMvc.perform(get("/" + shortCode).header("X-Forwarded-For", redirectIp))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.error").value("Too Many Requests"))
            .andExpect(jsonPath("$.message").value("Rate limit exceeded for redirect requests"))
            .andExpect(jsonPath("$.path").value("/" + shortCode));
    }

    private org.springframework.test.web.servlet.ResultActions createUrl(String clientIp, String originalUrl) throws Exception {
        return mockMvc.perform(post("/api/urls")
            .header("X-Forwarded-For", clientIp)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "originalUrl": "%s"
                }
                """.formatted(originalUrl)));
    }

    private String createShortCode(String clientIp, String originalUrl) throws Exception {
        MvcResult result = createUrl(clientIp, originalUrl)
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("shortCode").asText();
    }
}
