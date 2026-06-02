package com.example.demo.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = "teenyurlDatabaseUrl";
    private static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
    private static final String SPRING_DATASOURCE_USERNAME = "spring.datasource.username";
    private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String springDatasourceUrl = environment.getProperty(SPRING_DATASOURCE_URL);
        if (StringUtils.hasText(springDatasourceUrl) && springDatasourceUrl.startsWith("jdbc:")) {
            return;
        }

        String databaseUrl = firstPresent(
            springDatasourceUrl,
            environment.getProperty("DATABASE_URL"),
            environment.getProperty("TEENYURL_DATABASE_URL")
        );
        if (!StringUtils.hasText(databaseUrl) || databaseUrl.startsWith("jdbc:")) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        if (!"postgresql".equalsIgnoreCase(uri.getScheme()) && !"postgres".equalsIgnoreCase(uri.getScheme())) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(SPRING_DATASOURCE_URL, toJdbcUrl(uri));
        if (!StringUtils.hasText(environment.getProperty(SPRING_DATASOURCE_USERNAME))) {
            properties.put(SPRING_DATASOURCE_USERNAME, username(uri));
        }
        if (!StringUtils.hasText(environment.getProperty(SPRING_DATASOURCE_PASSWORD))) {
            properties.put(SPRING_DATASOURCE_PASSWORD, password(uri));
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    private static String firstPresent(String first, String second, String third) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return StringUtils.hasText(second) ? second : third;
    }

    private static String toJdbcUrl(URI uri) {
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
            .append(uri.getHost())
            .append(":")
            .append(port)
            .append(uri.getPath());
        if (StringUtils.hasText(uri.getQuery())) {
            jdbcUrl.append("?").append(uri.getQuery());
        }
        return jdbcUrl.toString();
    }

    private static String username(URI uri) {
        String userInfo = uri.getUserInfo();
        if (!StringUtils.hasText(userInfo)) {
            return "";
        }
        int separator = userInfo.indexOf(':');
        String value = separator == -1 ? userInfo : userInfo.substring(0, separator);
        return decode(value);
    }

    private static String password(URI uri) {
        String userInfo = uri.getUserInfo();
        if (!StringUtils.hasText(userInfo)) {
            return "";
        }
        int separator = userInfo.indexOf(':');
        if (separator == -1) {
            return "";
        }
        return decode(userInfo.substring(separator + 1));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
