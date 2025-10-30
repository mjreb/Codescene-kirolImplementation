package com.agent.infrastructure.llm;

import com.agent.domain.model.LLMProviderConfig;
import com.agent.domain.model.LLMRequest;
import com.agent.domain.model.LLMResponse;
import com.agent.domain.model.ProviderHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation for LLM providers with common functionality.
 */
public abstract class BaseLLMProvider implements LLMProvider {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final LLMProviderConfig config;
    
    public BaseLLMProvider(LLMProviderConfig config) {
        this.config = config;
    }
    
    @Override
    public LLMResponse generateResponse(LLMRequest request) {
        if (!config.isEnabled()) {
            throw new LLMProviderException("Provider " + getProviderId() + " is disabled");
        }
        
        return executeWithRetry(request);
    }
    
    @Override
    public ProviderHealth checkHealth() {
        ProviderHealth health = new ProviderHealth(getProviderId(), ProviderHealth.HealthStatus.UNKNOWN);
        
        try {
            long startTime = System.currentTimeMillis();
            boolean isHealthy = performHealthCheck();
            long responseTime = System.currentTimeMillis() - startTime;
            
            health.setStatus(isHealthy ? ProviderHealth.HealthStatus.HEALTHY : ProviderHealth.HealthStatus.UNHEALTHY);
            health.setResponseTimeMs(responseTime);
            health.setMessage(isHealthy ? "Provider is healthy" : "Provider health check failed");
            
        } catch (Exception e) {
            logger.warn("Health check failed for provider {}: {}", getProviderId(), e.getMessage());
            health.setStatus(ProviderHealth.HealthStatus.UNHEALTHY);
            health.setMessage("Health check exception: " + e.getMessage());
        }
        
        return health;
    }
    
    @Override
    public String getProviderId() {
        return config.getProviderId();
    }
    
    /**
     * Execute the request with retry logic.
     */
    private LLMResponse executeWithRetry(LLMRequest request) {
        LLMProviderConfig.RetryConfig retryConfig = config.getRetryConfig();
        if (retryConfig == null) {
            return doGenerateResponse(request);
        }
        
        Exception lastException = null;
        for (int attempt = 0; attempt <= retryConfig.getMaxRetries(); attempt++) {
            try {
                return doGenerateResponse(request);
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < retryConfig.getMaxRetries() && isRetryableException(e)) {
                    long delay = calculateDelay(attempt, retryConfig);
                    logger.warn("Request failed (attempt {}/{}), retrying in {}ms: {}", 
                               attempt + 1, retryConfig.getMaxRetries() + 1, delay, e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LLMProviderException("Request interrupted", ie);
                    }
                } else {
                    break;
                }
            }
        }
        
        throw new LLMProviderException("Request failed after " + (retryConfig.getMaxRetries() + 1) + " attempts", lastException);
    }
    
    /**
     * Calculate delay for retry with exponential backoff.
     */
    private long calculateDelay(int attempt, LLMProviderConfig.RetryConfig retryConfig) {
        long delay = (long) (retryConfig.getInitialDelayMs() * Math.pow(retryConfig.getBackoffMultiplier(), attempt));
        return Math.min(delay, retryConfig.getMaxDelayMs());
    }
    
    /**
     * Check if an exception is retryable.
     */
    protected boolean isRetryableException(Exception e) {
        // Default implementation - can be overridden by specific providers
        return e instanceof java.net.SocketTimeoutException ||
               e instanceof java.net.ConnectException ||
               (e instanceof LLMProviderException && ((LLMProviderException) e).isRetryable());
    }
    
    /**
     * Perform the actual LLM request - to be implemented by specific providers.
     */
    protected abstract LLMResponse doGenerateResponse(LLMRequest request);
    
    /**
     * Perform provider-specific health check.
     */
    protected abstract boolean performHealthCheck();
    
    /**
     * Get the configuration for this provider.
     */
    protected LLMProviderConfig getConfig() {
        return config;
    }
}