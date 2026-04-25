package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.SupportTestConfiguration;
import com.example.demo.repository.UrlDailyClickRepository;
import com.example.demo.repository.UrlMappingRepository;
import com.example.demo.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "teenyurl.security.create-api-key=test-secret")
@AutoConfigureMockMvc
@Import(SupportTestConfiguration.class)
class ApiKeyCreateUrlIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlMappingRepository repository;

    @Autowired
    private UrlDailyClickRepository dailyClickRepository;

    @BeforeEach
    void setUp() {
        dailyClickRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void rejectsCreateRequestWithoutApiKeyWhenConfigured() throws Exception {
        createUrlWithoutApiKey()
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Valid API key is required to create short URLs"));
    }

    @Test
    void acceptsCreateRequestWithApiKeyWhenConfigured() throws Exception {
        mockMvc.perform(post("/api/urls")
                .header(ApiKeyService.API_KEY_HEADER, "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "originalUrl": "https://example.com/protected-create"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isNotEmpty())
            .andExpect(jsonPath("$.originalUrl").value("https://example.com/protected-create"));
    }

    private org.springframework.test.web.servlet.ResultActions createUrlWithoutApiKey() throws Exception {
        return mockMvc.perform(post("/api/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "originalUrl": "https://example.com/protected-create"
                }
                """));
    }
}
