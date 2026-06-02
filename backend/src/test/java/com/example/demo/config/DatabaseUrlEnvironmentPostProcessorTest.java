package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {
    private final DatabaseUrlEnvironmentPostProcessor postProcessor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void convertsRenderPostgresUrlToSpringDatasourceProperties() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "DATABASE_URL", "postgresql://teenyurl_user:secret-password@dpg-example-a/teenyurl"
        )));

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://dpg-example-a:5432/teenyurl");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("teenyurl_user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("secret-password");
    }

    @Test
    void keepsExplicitSpringDatasourceUrl() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "DATABASE_URL", "postgresql://teenyurl_user:secret-password@dpg-example-a/teenyurl",
            "spring.datasource.url", "jdbc:postgresql://localhost:5432/local"
        )));

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://localhost:5432/local");
    }
}
