package com.agent.infrastructure.fallback;

import com.agent.infrastructure.resilience.ResilienceService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service that monitors and reports system status for graceful degradation.
 */
@Service
public class SystemStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemStatusService.class);
    
    private final ResilienceService resilienceService;
    private final AtomicReference<SystemStatus> currentStatus = new AtomicReference<>();
    
    public SystemStatusService(ResilienceService resilienceService) {
        this.resilienceService = resilienceService;
        this.currentStatus.set(new SystemStatus(true, true, true, true, Instant.now()));
    }
    
    /**
     * Get current system status.
     */
    public SystemStatus getCurrentStatus() {
        updateSystemStatus();
        return currentStatus.get();
    }
    
    /**
     * Update system status based on circuit breaker states and other indicators.
     */
    private void updateSystemStatus() {
        try {
            ResilienceService.CircuitBreakerStatus cbStatus = resilienceService.getCircuitBreakerStatus();
            
            boolean llmProvidersOperational = cbStatus.getLlmProviderState() != CircuitBreaker.State.OPEN;
            boolean externalServicesOperational = cbStatus.getExternalServiceState() != CircuitBreaker.State.OPEN;
            boolean databaseOperational = cbStatus.getDatabaseState() != CircuitBreaker.State.OPEN;
            boolean toolsOperational = true; // Tools don't have circuit breakers, assume operational
            
            SystemStatus newStatus = new SystemStatus(
                    llmProvidersOperational,
                    externalServicesOperational,
                    databaseOperational,
                    toolsOperational,
                    Instant.now()
            );
            
            SystemStatus oldStatus = currentStatus.getAndSet(newStatus);
            
            // Log status changes
            if (!newStatus.equals(oldStatus)) {
                logger.info("System status changed: {}", newStatus);
            }
            
        } catch (Exception e) {
            logger.error("Error updating system status", e);
        }
    }
    
    /**
     * Force update of system status.
     */
    public void refreshStatus() {
        updateSystemStatus();
    }
    
    /**
     * Check if specific component is operational.
     */
    public boolean isComponentOperational(String component) {
        SystemStatus status = getCurrentStatus();
        
        switch (component.toLowerCase()) {
            case "llm":
            case "llmprovider":
                return status.isLLMProvidersOperational();
            case "external":
            case "externalservice":
                return status.isExternalServicesOperational();
            case "database":
            case "db":
                return status.isDatabaseOperational();
            case "tools":
            case "tool":
                return status.isToolsOperational();
            default:
                return status.isFullyOperational();
        }
    }
    
    /**
     * Get degradation level (0 = fully operational, 1 = fully degraded).
     */
    public double getDegradationLevel() {
        SystemStatus status = getCurrentStatus();
        
        int totalComponents = 4;
        int operationalComponents = 0;
        
        if (status.isLLMProvidersOperational()) operationalComponents++;
        if (status.isExternalServicesOperational()) operationalComponents++;
        if (status.isDatabaseOperational()) operationalComponents++;
        if (status.isToolsOperational()) operationalComponents++;
        
        return 1.0 - ((double) operationalComponents / totalComponents);
    }
    
    /**
     * Get human-readable status description.
     */
    public String getStatusDescription() {
        SystemStatus status = getCurrentStatus();
        
        if (status.isFullyOperational()) {
            return "All systems operational";
        }
        
        double degradationLevel = getDegradationLevel();
        
        if (degradationLevel < 0.25) {
            return "Minor service disruption";
        } else if (degradationLevel < 0.5) {
            return "Partial service degradation";
        } else if (degradationLevel < 0.75) {
            return "Major service disruption";
        } else {
            return "Severe service degradation";
        }
    }
}

