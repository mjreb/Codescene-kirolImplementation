package com.agent.infrastructure.monitoring.health;

import com.agent.domain.interfaces.LLMProviderManager;
import com.agent.domain.model.ProviderHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Health indicator for LLM providers.
 */
@Component
public class LLMProviderHealthIndicator implements HealthIndicator {
    
    private final LLMProviderManager llmProviderManager;
    
    public LLMProviderHealthIndicator(LLMProviderManager llmProviderManager) {
        this.llmProviderManager = llmProviderManager;
    }
    
    @Override
    public Health health() {
        try {
            List<String> availableProviders = llmProviderManager.getAvailableProviders();
            
            if (availableProviders.isEmpty()) {
                return Health.down()
                        .withDetail("reason", "No LLM providers available")
                        .build();
            }
            
            Map<String, Object> providerHealthMap = availableProviders.stream()
                    .collect(Collectors.toMap(
                            providerId -> providerId,
                            providerId -> {
                                try {
                                    ProviderHealth health = llmProviderManager.checkProviderHealth(providerId);
                                    boolean isHealthy = health.getStatus() == ProviderHealth.HealthStatus.HEALTHY;
                                    return Map.of(
                                            "status", isHealthy ? "UP" : "DOWN",
                                            "responseTime", health.getResponseTimeMs() + "ms",
                                            "lastCheck", health.getLastCheckTime(),
                                            "errorMessage", health.getErrorMessage() != null ? health.getErrorMessage() : "None"
                                    );
                                } catch (Exception e) {
                                    return Map.of(
                                            "status", "DOWN",
                                            "error", e.getMessage()
                                    );
                                }
                            }
                    ));
            
            // Check if at least one provider is healthy
            boolean anyHealthy = providerHealthMap.values().stream()
                    .anyMatch(health -> {
                        if (health instanceof Map<?, ?> healthMap) {
                            return "UP".equals(healthMap.get("status"));
                        }
                        return false;
                    });
            
            Health.Builder healthBuilder = anyHealthy ? Health.up() : Health.down();
            
            return healthBuilder
                    .withDetail("providers", providerHealthMap)
                    .withDetail("totalProviders", availableProviders.size())
                    .withDetail("healthyProviders", providerHealthMap.values().stream()
                            .mapToInt(health -> {
                                if (health instanceof Map<?, ?> healthMap) {
                                    return "UP".equals(healthMap.get("status")) ? 1 : 0;
                                }
                                return 0;
                            }).sum())
                    .build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}