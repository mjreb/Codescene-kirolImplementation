package com.agent.presentation.controller;

import com.agent.application.service.HealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for health check endpoints.
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    private final HealthService healthService;
    
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }
    
    /**
     * Basic health check endpoint.
     * GET /health
     */
    @GetMapping
    public ResponseEntity<HealthService.HealthStatus> getHealth() {
        HealthService.HealthStatus health = healthService.getBasicHealth();
        
        HttpStatus httpStatus = switch (health.getStatus()) {
            case "UP" -> HttpStatus.OK;
            case "DEGRADED" -> HttpStatus.OK; // Still operational
            case "DOWN" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        return ResponseEntity.status(httpStatus).body(health);
    }
    
    /**
     * Detailed health check endpoint with component breakdown.
     * GET /health/detailed
     */
    @GetMapping("/detailed")
    public ResponseEntity<HealthService.DetailedHealthStatus> getDetailedHealth() {
        HealthService.DetailedHealthStatus health = healthService.getDetailedHealth();
        
        HttpStatus httpStatus = switch (health.getStatus()) {
            case "UP" -> HttpStatus.OK;
            case "DEGRADED" -> HttpStatus.OK; // Still operational
            case "DOWN" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        return ResponseEntity.status(httpStatus).body(health);
    }
}