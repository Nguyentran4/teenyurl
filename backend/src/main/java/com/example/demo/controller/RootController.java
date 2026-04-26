package com.example.demo.controller;

import com.example.demo.dto.ServiceStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public ServiceStatusResponse status() {
        return new ServiceStatusResponse(
            "TeenyURL",
            "running",
            "Use POST /api/urls to create short URLs"
        );
    }
}
