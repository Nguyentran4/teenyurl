package com.example.demo.controller;

import com.example.demo.dto.RedirectRequestMetadata;
import com.example.demo.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {
    private final UrlShortenerService urlShortenerService;

    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String originalUrl = urlShortenerService.resolveOriginalUrl(shortCode, extractMetadata(request));
        return ResponseEntity
            .status(302)
            .location(URI.create(originalUrl))
            .build();
    }

    private RedirectRequestMetadata extractMetadata(HttpServletRequest request) {
        return new RedirectRequestMetadata(
            request.getHeader("User-Agent"),
            request.getHeader("Referer"),
            extractClientIp(request)
        );
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }

        return forwardedFor.split(",")[0].trim();
    }
}
