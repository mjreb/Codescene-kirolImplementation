package com.agent.application.service;

import com.agent.domain.interfaces.LLMProviderManager;
import com.agent.domain.interfaces.MemoryManager;
import com.agent.domain.model.ProviderHealth;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for checking system health and dependencies.
 */
@Service
public class HealthService {
    
    private final LLMProviderManager llmProviderManager;
    private final MemoryManager memoryManager;
    
    public HealthService(LLMProviderManager llmProviderManager, 
                        MemoryManager memoryManager) {
        this.llmProviderManager = llmProviderManager;
        this.memoryManager = memoryManager;
    }
    
    /**
     * Get basic health status.
     */
    public HealthStatus getBasicHealth() {
        HealthStatus status = new HealthStatus();
        status.setStatus("UP");
        status.setTimestamp(Instant.now());
        
        // Check critical dependencies
        boolean allHealthy = true;
        
        // Check LLM providers
        try {
            List<String> providers = llmProviderManager.getAvailableProviders();
            if (providers.isEmpty()) {
                allHealthy = false;
                status.setStatus("DOWN");
                status.addDetail("llm_providers", "No LLM providers available");
            } else {
                status.addDetail("llm_providers", providers.size() + " providers available");
            }
        } catch (Exception e) {
            allHealthy = false;
            status.setStatus("DOWN");
            status.addDetail("llm_providers", "Error checking providers: " + e.getMessage());
        }
        
        // Check memory manager
        try {
            // Simple test to verify memory manager is working
            memoryManager.cleanupExpiredMemory();
            status.addDetail("memory_manager", "OK");
        } catch (Exception e) {
            allHealthy = false;
            status.setStatus("DOWN");
            status.addDetail("memory_manager", "Error: " + e.getMessage());
        }
        
        if (!allHealthy && "UP".equals(status.getStatus())) {
            status.setStatus("DEGRADED");
        }
        
        return status;
    }
    
    /**
     * Get detailed health status with dependency checks.
     */
    public DetailedHealthStatus getDetailedHealth() {
        DetailedHealthStatus status = new DetailedHealthStatus();
        status.setStatus("UP");
        status.setTimestamp(Instant.now());
        
        // Check LLM providers in detail
        Map<String, Object> llmStatus = checkLLMProviders();
        status.addComponent("llm_providers", llmStatus);
        
        // Check memory components
        Map<String, Object> memoryStatus = checkMemoryComponents();
        status.addComponent("memory", memoryStatus);
        
        // Check database connectivity (through memory manager)
        Map<String, Object> databaseStatus = checkDatabaseConnectivity();
        status.addComponent("database", databaseStatus);
        
        // Check Redis connectivity (through memory manager)
        Map<String, Object> redisStatus = checkRedisConnectivity();
        status.addComponent("redis", redisStatus);
        
        // Determine overall status
        boolean hasDown = status.getComponents().values().stream()
                .anyMatch(component -> {
                    if (component instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> comp = (Map<String, Object>) component;
                        return "DOWN".equals(comp.get("status"));
                    }
                    return false;
                });
        
        boolean hasDegraded = status.getComponents().values().stream()
                .anyMatch(component -> {
                    if (component instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> comp = (Map<String, Object>) component;
                        return "DEGRADED".equals(comp.get("status"));
                    }
                    return false;
                });
        
        if (hasDown) {
            status.setStatus("DOWN");
        } else if (hasDegraded) {
            status.setStatus("DEGRADED");
        }
        
        return status;
    }
    
    private Map<String, Object> checkLLMProviders() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            List<String> providers = llmProviderManager.getAvailableProviders();
            Map<String, Object> providerDetails = new HashMap<>();
            
            boolean allHealthy = true;
            for (String providerId : providers) {
                try {
                    ProviderHealth health = llmProviderManager.checkProviderHealth(providerId);
                    Map<String, Object> providerStatus = new HashMap<>();
                    providerStatus.put("status", health.getStatus().toString());
                    providerStatus.put("responseTime", health.getResponseTimeMs());
                    providerStatus.put("lastCheck", health.getLastCheckTime());
                    
                    if (health.getStatus() == ProviderHealth.HealthStatus.UNHEALTHY) {
                        allHealthy = false;
                        providerStatus.put("error", health.getErrorMessage());
                    }
                    
                    providerDetails.put(providerId, providerStatus);
                } catch (Exception e) {
                    allHealthy = false;
                    Map<String, Object> errorStatus = new HashMap<>();
                    errorStatus.put("status", "ERROR");
                    errorStatus.put("error", e.getMessage());
                    providerDetails.put(providerId, errorStatus);
                }
            }
            
            status.put("status", allHealthy ? "UP" : "DEGRADED");
            status.put("providers", providerDetails);
            status.put("totalProviders", providers.size());
            
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    private Map<String, Object> checkMemoryComponents() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test memory manager functionality
            memoryManager.cleanupExpiredMemory();
            status.put("status", "UP");
            status.put("cleanup", "OK");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    private Map<String, Object> checkDatabaseConnectivity() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test database connectivity through memory manager
            // This is a simple test - in a real implementation you might want more specific checks
            memoryManager.retrieveLongTermMemory("health_check_test");
            status.put("status", "UP");
            status.put("connectivity", "OK");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    private Map<String, Object> checkRedisConnectivity() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test Redis connectivity through memory manager
            // This is a simple test - in a real implementation you might want more specific checks
            memoryManager.retrieveConversationContext("health_check_test");
            status.put("status", "UP");
            status.put("connectivity", "OK");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * Basic health status response.
     */
    public static class HealthStatus {
        private String status;
        private Instant timestamp;
        private Map<String, Object> details = new HashMap<>();
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        
        public void addDetail(String key, Object value) {
            this.details.put(key, value);
        }
    }
    
    /**
     * Detailed health status response with component breakdown.
     */
    public static class DetailedHealthStatus {
        private String status;
        private Instant timestamp;
        private Map<String, Object> components = new HashMap<>();
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getComponents() { return components; }
        public void setComponents(Map<String, Object> components) { this.components = components; }
        
        public void addComponent(String name, Object status) {
            this.components.put(name, status);
        }
    }
}