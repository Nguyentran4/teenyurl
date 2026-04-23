package com.example.demo.config;

import java.time.Clock;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ApplicationConfig {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean(name = "analyticsTaskExecutor")
    public Executor analyticsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1_000);
        executor.setThreadNamePrefix("teenyurl-analytics-");
        executor.initialize();
        return executor;
    }
}
