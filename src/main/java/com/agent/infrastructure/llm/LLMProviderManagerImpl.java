package com.agent.infrastructure.llm;

import com.agent.domain.interfaces.LLMProviderManager;
import com.agent.domain.model.LLMRequest;
import com.agent.domain.model.LLMResponse;
import com.agent.domain.model.ProviderHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of LLMProviderManager that manages multiple LLM providers
 * and handles provider selection, failover, and health monitoring.
 */
@Service
public class LLMProviderManagerImpl implements LLMProviderManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMProviderManagerImpl.class);
    
    private final Map<String, LLMProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, ProviderHealth> healthCache = new ConcurrentHashMap<>();
    
    /**
     * Register an LLM provider.
     */
    public void registerProvider(LLMProvider provider) {
        providers.put(provider.getProviderId(), provider);
        logger.info("Registered LLM provider: {}", provider.getProviderId());
    }
    
    /**
     * Unregister an LLM provider.
     */
    public void unregisterProvider(String providerId) {
        LLMProvider removed = providers.remove(providerId);
        healthCache.remove(providerId);
        if (removed != null) {
            logger.info("Unregistered LLM provider: {}", providerId);
        }
    }
    
    @Override
    public LLMResponse generateResponse(LLMRequest request, String providerId) {
        if (providerId == null || providerId.trim().isEmpty()) {
            return generateResponseWithFailover(request);
        }
        
        LLMProvider provider = providers.get(providerId);
        if (provider == null) {
            throw new LLMProviderException("Provider not found: " + providerId);
        }
        
        try {
            return provider.generateResponse(request);
        } catch (Exception e) {
            logger.error("Failed to generate response with provider {}: {}", providerId, e.getMessage());
            
            // Try failover if the specific provider failed
            if (providers.size() > 1) {
                logger.info("Attempting failover for failed provider: {}", providerId);
                return generateResponseWithFailover(request, Collections.singleton(providerId));
            }
            
            throw e;
        }
    }
    
    /**
     * Generate response with automatic provider selection and failover.
     */
    private LLMResponse generateResponseWithFailover(LLMRequest request) {
        return generateResponseWithFailover(request, Collections.emptySet());
    }
    
    /**
     * Generate response with failover, excluding specified providers.
     */
    private LLMResponse generateResponseWithFailover(LLMRequest request, Set<String> excludeProviders) {
        List<LLMProvider> availableProviders = getHealthyProviders(request.getModel(), excludeProviders);
        
        if (availableProviders.isEmpty()) {
            throw new LLMProviderException("No healthy providers available for model: " + request.getModel());
        }
        
        Exception lastException = null;
        
        for (LLMProvider provider : availableProviders) {
            try {
                logger.debug("Attempting request with provider: {}", provider.getProviderId());
                return provider.generateResponse(request);
            } catch (Exception e) {
                logger.warn("Provider {} failed: {}", provider.getProviderId(), e.getMessage());
                lastException = e;
                
                // Update health cache to mark provider as potentially unhealthy
                ProviderHealth health = new ProviderHealth(provider.getProviderId(), ProviderHealth.HealthStatus.DEGRADED);
                health.setMessage("Recent request failed: " + e.getMessage());
                healthCache.put(provider.getProviderId(), health);
            }
        }
        
        throw new LLMProviderException("All providers failed for request", lastException);
    }
    
    /**
     * Get list of healthy providers that support the given model.
     */
    private List<LLMProvider> getHealthyProviders(String model, Set<String> excludeProviders) {
        return providers.values().stream()
                .filter(provider -> !excludeProviders.contains(provider.getProviderId()))
                .filter(provider -> model == null || provider.supportsModel(model))
                .filter(this::isProviderHealthy)
                .sorted(this::compareProviderPriority)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a provider is considered healthy.
     */
    private boolean isProviderHealthy(LLMProvider provider) {
        ProviderHealth health = healthCache.get(provider.getProviderId());
        if (health == null) {
            // No cached health info, assume healthy
            return true;
        }
        
        // Consider provider healthy if not explicitly unhealthy
        return health.getStatus() != ProviderHealth.HealthStatus.UNHEALTHY;
    }
    
    /**
     * Compare providers by priority (lower number = higher priority).
     */
    private int compareProviderPriority(LLMProvider p1, LLMProvider p2) {
        // For now, just use alphabetical order
        // In a real implementation, this would use provider configuration priority
        return p1.getProviderId().compareTo(p2.getProviderId());
    }
    
    @Override
    public List<String> getAvailableProviders() {
        return new ArrayList<>(providers.keySet());
    }
    
    @Override
    public ProviderHealth checkProviderHealth(String providerId) {
        LLMProvider provider = providers.get(providerId);
        if (provider == null) {
            ProviderHealth health = new ProviderHealth(providerId, ProviderHealth.HealthStatus.UNKNOWN);
            health.setMessage("Provider not found");
            return health;
        }
        
        try {
            ProviderHealth health = provider.checkHealth();
            healthCache.put(providerId, health);
            return health;
        } catch (Exception e) {
            logger.error("Health check failed for provider {}: {}", providerId, e.getMessage());
            ProviderHealth health = new ProviderHealth(providerId, ProviderHealth.HealthStatus.UNHEALTHY);
            health.setMessage("Health check exception: " + e.getMessage());
            healthCache.put(providerId, health);
            return health;
        }
    }
    
    /**
     * Check health of all providers.
     */
    public Map<String, ProviderHealth> checkAllProvidersHealth() {
        Map<String, ProviderHealth> results = new HashMap<>();
        
        for (String providerId : providers.keySet()) {
            results.put(providerId, checkProviderHealth(providerId));
        }
        
        return results;
    }
    
    /**
     * Get cached health information for all providers.
     */
    public Map<String, ProviderHealth> getCachedHealthStatus() {
        return new HashMap<>(healthCache);
    }
}