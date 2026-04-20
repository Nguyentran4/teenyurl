package com.example.demo.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void returnsNotFoundForMissingShortCode() throws Exception {
        mockMvc.perform(get("/missing-code"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Short URL not found: missing-code"));
    }
}
