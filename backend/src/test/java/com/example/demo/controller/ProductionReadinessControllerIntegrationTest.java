package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.SupportTestConfiguration;
import com.example.demo.repository.UrlDailyClickRepository;
import com.example.demo.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SupportTestConfiguration.class)
class ProductionReadinessControllerIntegrationTest {
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
    void returnsHealthWithDatabaseAndRedisStatus() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.checkedAt").isNotEmpty())
            .andExpect(jsonPath("$.components.database.status").value("UP"))
            .andExpect(jsonPath("$.components.redis.status").value("DISABLED"));
    }

    @Test
    void returnsBasicMetrics() throws Exception {
        mockMvc.perform(get("/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startedAt").isNotEmpty())
            .andExpect(jsonPath("$.uptimeSeconds").isNumber())
            .andExpect(jsonPath("$.urls.totalUrls").value(0))
            .andExpect(jsonPath("$.urls.totalClicks").value(0))
            .andExpect(jsonPath("$.runtime.availableProcessors").isNumber())
            .andExpect(jsonPath("$.runtime.usedMemoryBytes").isNumber())
            .andExpect(jsonPath("$.runtime.maxMemoryBytes").isNumber());
    }
}
