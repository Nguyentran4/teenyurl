package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.service.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("render")
@SpringBootTest(properties = {
    "REDIS_URL=redis://localhost:6379",
    "teenyurl.cache.redis.enabled=false",
    "teenyurl.rate-limit.redis.enabled=true"
})
class RenderProfileContextTest {
    @Autowired
    private RateLimiter rateLimiter;

    @Test
    void contextLoadsWithRenderProfile() {
        assertThat(rateLimiter).isNotNull();
    }
}
