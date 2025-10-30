package com.agent.presentation.controller;

import com.agent.infrastructure.fallback.SystemStatus;
import com.agent.infrastructure.fallback.SystemStatusService;
import com.agent.infrastructure.resilience.ResilienceService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Controller for exposing system status and degradation information.
 */
@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
    
    private final SystemStatusService systemStatusService;
    private final ResilienceService resilienceService;
    
    public SystemStatusController(SystemStatusService systemStatusService, ResilienceService resilienceService) {
        this.systemStatusService = systemStatusService;
        this.resilienceService = resilienceService;
    }
    
    /**
     * Get current system status.
     */
    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        SystemStatus status = systemStatusService.getCurrentStatus();
        
        SystemStatusResponse response = new SystemStatusResponse(
                status.isFullyOperational(),
                systemStatusService.getStatusDescription(),
                systemStatusService.getDegradationLevel(),
                status.isLLMProvidersOperational(),
                status.isExternalServicesOperational(),
                status.isDatabaseOperational(),
                status.isToolsOperational(),
                status.getLastUpdated()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get circuit breaker status.
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<CircuitBreakerStatusResponse> getCircuitBreakerStatus() {
        ResilienceService.CircuitBreakerStatus status = resilienceService.getCircuitBreakerStatus();
        
        CircuitBreakerStatusResponse response = new CircuitBreakerStatusResponse(
                status.getLlmProviderState(),
                status.getExternalServiceState(),
                status.getDatabaseState(),
                status.isAnyCircuitOpen()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get system health summary.
     */
    @GetMapping("/health-summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        SystemStatus status = systemStatusService.getCurrentStatus();
        ResilienceService.CircuitBreakerStatus cbStatus = resilienceService.getCircuitBreakerStatus();
        
        Map<String, Object> summary = Map.of(
                "overall_status", status.isFullyOperational() ? "healthy" : "degraded",
                "description", systemStatusService.getStatusDescription(),
                "degradation_level", systemStatusService.getDegradationLevel(),
                "components", Map.of(
                        "llm_providers", Map.of(
                                "operational", status.isLLMProvidersOperational(),
                                "circuit_breaker", cbStatus.getLlmProviderState().toString()
                        ),
                        "external_services", Map.of(
                                "operational", status.isExternalServicesOperational(),
                                "circuit_breaker", cbStatus.getExternalServiceState().toString()
                        ),
                        "database", Map.of(
                                "operational", status.isDatabaseOperational(),
                                "circuit_breaker", cbStatus.getDatabaseState().toString()
                        ),
                        "tools", Map.of(
                                "operational", status.isToolsOperational()
                        )
                ),
                "last_updated", status.getLastUpdated()
        );
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Force refresh of system status.
     */
    @GetMapping("/refresh")
    public ResponseEntity<String> refreshStatus() {
        systemStatusService.refreshStatus();
        return ResponseEntity.ok("System status refreshed");
    }
    
    // ========== Response DTOs ==========
    
    public static class SystemStatusResponse {
        private final boolean fullyOperational;
        private final String description;
        private final double degradationLevel;
        private final boolean llmProvidersOperational;
        private final boolean externalServicesOperational;
        private final boolean databaseOperational;
        private final boolean toolsOperational;
        private final Instant lastUpdated;
        
        public SystemStatusResponse(boolean fullyOperational, String description, double degradationLevel,
                                  boolean llmProvidersOperational, boolean externalServicesOperational,
                                  boolean databaseOperational, boolean toolsOperational, Instant lastUpdated) {
            this.fullyOperational = fullyOperational;
            this.description = description;
            this.degradationLevel = degradationLevel;
            this.llmProvidersOperational = llmProvidersOperational;
            this.externalServicesOperational = externalServicesOperational;
            this.databaseOperational = databaseOperational;
            this.toolsOperational = toolsOperational;
            this.lastUpdated = lastUpdated;
        }
        
        // Getters
        public boolean isFullyOperational() { return fullyOperational; }
        public String getDescription() { return description; }
        public double getDegradationLevel() { return degradationLevel; }
        public boolean isLlmProvidersOperational() { return llmProvidersOperational; }
        public boolean isExternalServicesOperational() { return externalServicesOperational; }
        public boolean isDatabaseOperational() { return databaseOperational; }
        public boolean isToolsOperational() { return toolsOperational; }
        public Instant getLastUpdated() { return lastUpdated; }
    }
    
    public static class CircuitBreakerStatusResponse {
        private final CircuitBreaker.State llmProviderState;
        private final CircuitBreaker.State externalServiceState;
        private final CircuitBreaker.State databaseState;
        private final boolean anyCircuitOpen;
        
        public CircuitBreakerStatusResponse(CircuitBreaker.State llmProviderState,
                                          CircuitBreaker.State externalServiceState,
                                          CircuitBreaker.State databaseState,
                                          boolean anyCircuitOpen) {
            this.llmProviderState = llmProviderState;
            this.externalServiceState = externalServiceState;
            this.databaseState = databaseState;
            this.anyCircuitOpen = anyCircuitOpen;
        }
        
        // Getters
        public CircuitBreaker.State getLlmProviderState() { return llmProviderState; }
        public CircuitBreaker.State getExternalServiceState() { return externalServiceState; }
        public CircuitBreaker.State getDatabaseState() { return databaseState; }
        public boolean isAnyCircuitOpen() { return anyCircuitOpen; }
    }
}