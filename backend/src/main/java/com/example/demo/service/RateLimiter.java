package com.example.demo.service;

public interface RateLimiter {
    void checkCreateAllowed(String clientIp);

    void checkRedirectAllowed(String clientIp);
}
