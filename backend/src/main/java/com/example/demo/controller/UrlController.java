package com.example.demo.controller;

import com.example.demo.dto.CreateUrlRequest;
import com.example.demo.dto.CreateUrlResponse;
import com.example.demo.dto.UrlStatsResponse;
import com.example.demo.service.ClientIpExtractor;
import com.example.demo.service.RateLimiter;
import com.example.demo.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
public class UrlController {
    private final UrlShortenerService urlShortenerService;
    private final RateLimiter rateLimiter;
    private final ClientIpExtractor clientIpExtractor;

    public UrlController(
        UrlShortenerService urlShortenerService,
        RateLimiter rateLimiter,
        ClientIpExtractor clientIpExtractor
    ) {
        this.urlShortenerService = urlShortenerService;
        this.rateLimiter = rateLimiter;
        this.clientIpExtractor = clientIpExtractor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUrlResponse createShortUrl(@RequestBody CreateUrlRequest request, HttpServletRequest httpRequest) {
        rateLimiter.checkCreateAllowed(clientIpExtractor.extract(httpRequest));
        return urlShortenerService.createShortUrl(request);
    }

    @GetMapping("/{shortCode}/stats")
    public UrlStatsResponse getStats(@PathVariable String shortCode) {
        return urlShortenerService.getStats(shortCode);
    }
}
