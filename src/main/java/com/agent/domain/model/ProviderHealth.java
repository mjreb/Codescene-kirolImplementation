package com.agent.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the health status of an LLM provider.
 */
public class ProviderHealth {
    private String providerId;
    private HealthStatus status;
    private String message;
    private long responseTimeMs;
    private Instant lastChecked;
    private Map<String, Object> details;
    
    public ProviderHealth() {
        this.lastChecked = Instant.now();
    }
    
    public ProviderHealth(String providerId, HealthStatus status) {
        this();
        this.providerId = providerId;
        this.status = status;
    }
    
    // Getters and setters
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    
    public HealthStatus getStatus() { return status; }
    public void setStatus(HealthStatus status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    
    public Instant getLastChecked() { return lastChecked; }
    public void setLastChecked(Instant lastChecked) { this.lastChecked = lastChecked; }
    
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    
    // Alias methods for compatibility
    public Instant getLastCheckTime() { return lastChecked; }
    public String getErrorMessage() { return message; }
    
    public enum HealthStatus {
        HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN
    }
}