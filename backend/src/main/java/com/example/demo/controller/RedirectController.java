package com.example.demo.controller;

import com.example.demo.dto.RedirectRequestMetadata;
import com.example.demo.service.ClientIpExtractor;
import com.example.demo.service.RateLimiter;
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
    private final RateLimiter rateLimiter;
    private final ClientIpExtractor clientIpExtractor;

    public RedirectController(
        UrlShortenerService urlShortenerService,
        RateLimiter rateLimiter,
        ClientIpExtractor clientIpExtractor
    ) {
        this.urlShortenerService = urlShortenerService;
        this.rateLimiter = rateLimiter;
        this.clientIpExtractor = clientIpExtractor;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String clientIp = clientIpExtractor.extract(request);
        rateLimiter.checkRedirectAllowed(clientIp);
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
            clientIpExtractor.extract(request)
        );
    }
}
