package com.example.demo.service;

import com.example.demo.dto.HealthResponse;
import com.example.demo.dto.HealthResponse.ComponentHealth;
import com.example.demo.dto.MetricsResponse;
import com.example.demo.dto.MetricsResponse.RuntimeMetrics;
import com.example.demo.dto.MetricsResponse.UrlMetrics;
import com.example.demo.repository.UrlMappingRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductionReadinessService {
    private final DataSource dataSource;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final UrlMappingRepository urlMappingRepository;
    private final Clock clock;
    private final Instant startedAt;
    private final boolean redisRequired;

    public ProductionReadinessService(
        DataSource dataSource,
        ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
        UrlMappingRepository urlMappingRepository,
        Clock clock,
        @Value("${teenyurl.cache.redis.enabled:true}") boolean redisCacheEnabled,
        @Value("${teenyurl.rate-limit.redis.enabled:true}") boolean redisRateLimitEnabled
    ) {
        this.dataSource = dataSource;
        this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
        this.urlMappingRepository = urlMappingRepository;
        this.clock = clock;
        this.startedAt = clock.instant();
        this.redisRequired = redisCacheEnabled || redisRateLimitEnabled;
    }

    public HealthResponse health() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("database", checkDatabase());
        components.put("redis", checkRedis());

        boolean healthy = components.values().stream()
            .allMatch(component -> "UP".equals(component.status()) || "DISABLED".equals(component.status()));

        return new HealthResponse(healthy ? "UP" : "DOWN", clock.instant(), components);
    }

    public MetricsResponse metrics() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBytes = runtime.totalMemory() - runtime.freeMemory();

        return new MetricsResponse(
            startedAt,
            MetricsResponse.uptimeSeconds(startedAt, clock.instant()),
            new UrlMetrics(urlMappingRepository.count(), urlMappingRepository.sumClickCount()),
            new RuntimeMetrics(runtime.availableProcessors(), usedMemoryBytes, runtime.maxMemory())
        );
    }

    private ComponentHealth checkDatabase() {
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("select 1");
            ResultSet resultSet = statement.executeQuery()
        ) {
            return resultSet.next()
                ? new ComponentHealth("UP", "Connection validated")
                : new ComponentHealth("DOWN", "Validation query returned no rows");
        } catch (Exception exception) {
            return new ComponentHealth("DOWN", exception.getMessage());
        }
    }

    private ComponentHealth checkRedis() {
        if (!redisRequired) {
            return new ComponentHealth("DISABLED", "Redis-backed features are disabled");
        }

        RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getIfAvailable();
        if (connectionFactory == null) {
            return new ComponentHealth("DOWN", "Redis connection factory is unavailable");
        }

        try (RedisConnection connection = connectionFactory.getConnection()) {
            String response = connection.ping();
            return "PONG".equals(response)
                ? new ComponentHealth("UP", "Connection validated")
                : new ComponentHealth("DOWN", "Unexpected ping response: " + response);
        } catch (RedisConnectionFailureException exception) {
            return new ComponentHealth("DOWN", exception.getMessage());
        } catch (Exception exception) {
            return new ComponentHealth("DOWN", exception.getMessage());
        }
    }
}
