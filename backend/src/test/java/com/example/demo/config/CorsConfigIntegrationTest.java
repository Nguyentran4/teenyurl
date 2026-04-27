package com.example.demo.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.SupportTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SupportTestConfiguration.class)
class CorsConfigIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsConfiguredFrontendOriginForCreateRequests() throws Exception {
        mockMvc.perform(options("/api/urls")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST"))
            .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type"));
    }

    @Test
    void rejectsUnconfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/urls")
                .header("Origin", "https://not-trusted.example.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
            .andExpect(status().isForbidden());
    }
}
