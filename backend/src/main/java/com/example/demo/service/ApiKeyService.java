package com.example.demo.service;

import com.example.demo.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {
    public static final String API_KEY_HEADER = "X-API-Key";

    private final byte[] requiredApiKey;

    public ApiKeyService(@Value("${teenyurl.security.create-api-key:}") String requiredApiKey) {
        this.requiredApiKey = normalize(requiredApiKey).getBytes(StandardCharsets.UTF_8);
    }

    public void validateCreateRequest(String submittedApiKey) {
        if (requiredApiKey.length == 0) {
            return;
        }

        byte[] submitted = normalize(submittedApiKey).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(requiredApiKey, submitted)) {
            throw new UnauthorizedException("Valid API key is required to create short URLs");
        }
    }

    private String normalize(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }
}
