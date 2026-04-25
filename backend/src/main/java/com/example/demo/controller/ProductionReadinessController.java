package com.example.demo.controller;

import com.example.demo.dto.HealthResponse;
import com.example.demo.dto.MetricsResponse;
import com.example.demo.service.ProductionReadinessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductionReadinessController {
    private final ProductionReadinessService productionReadinessService;

    public ProductionReadinessController(ProductionReadinessService productionReadinessService) {
        this.productionReadinessService = productionReadinessService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = productionReadinessService.health();
        HttpStatus status = response.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/metrics")
    public MetricsResponse metrics() {
        return productionReadinessService.metrics();
    }
}
