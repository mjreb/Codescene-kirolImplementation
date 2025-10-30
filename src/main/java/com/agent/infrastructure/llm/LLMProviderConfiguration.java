package com.agent.infrastructure.llm;

import com.agent.domain.model.LLMProviderConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for LLM providers.
 */
@Configuration
@ConfigurationProperties(prefix = "agent.llm")
public class LLMProviderConfiguration {
    
    private Map<String, ProviderSettings> providers = new HashMap<>();
    
    public Map<String, ProviderSettings> getProviders() {
        return providers;
    }
    
    public void setProviders(Map<String, ProviderSettings> providers) {
        this.providers = providers;
    }
    
    /**
     * Settings for individual provider configuration.
     */
    public static class ProviderSettings {
        private String name;
        private String apiKey;
        private String baseUrl;
        private boolean enabled = true;
        private int priority = 1;
        private int timeoutSeconds = 30;
        private Map<String, String> headers = new HashMap<>();
        private RetrySettings retry = new RetrySettings();
        private RateLimitSettings rateLimit = new RateLimitSettings();
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public RetrySettings getRetry() { return retry; }
        public void setRetry(RetrySettings retry) { this.retry = retry; }
        
        public RateLimitSettings getRateLimit() { return rateLimit; }
        public void setRateLimit(RateLimitSettings rateLimit) { this.rateLimit = rateLimit; }
        
        /**
         * Convert to domain model.
         */
        public LLMProviderConfig toLLMProviderConfig(String providerId) {
            LLMProviderConfig config = new LLMProviderConfig(providerId, name, apiKey);
            config.setBaseUrl(baseUrl);
            config.setEnabled(enabled);
            config.setPriority(priority);
            config.setTimeoutSeconds(timeoutSeconds);
            config.setHeaders(headers);
            
            if (retry != null) {
                LLMProviderConfig.RetryConfig retryConfig = new LLMProviderConfig.RetryConfig();
                retryConfig.setMaxRetries(retry.getMaxRetries());
                retryConfig.setInitialDelayMs(retry.getInitialDelayMs());
                retryConfig.setBackoffMultiplier(retry.getBackoffMultiplier());
                retryConfig.setMaxDelayMs(retry.getMaxDelayMs());
                config.setRetryConfig(retryConfig);
            }
            
            if (rateLimit != null) {
                LLMProviderConfig.RateLimitConfig rateLimitConfig = new LLMProviderConfig.RateLimitConfig();
                rateLimitConfig.setRequestsPerMinute(rateLimit.getRequestsPerMinute());
                rateLimitConfig.setRequestsPerHour(rateLimit.getRequestsPerHour());
                rateLimitConfig.setTokensPerMinute(rateLimit.getTokensPerMinute());
                rateLimitConfig.setEnabled(rateLimit.isEnabled());
                config.setRateLimitConfig(rateLimitConfig);
            }
            
            return config;
        }
    }
    
    /**
     * Retry configuration settings.
     */
    public static class RetrySettings {
        private int maxRetries = 3;
        private long initialDelayMs = 1000;
        private double backoffMultiplier = 2.0;
        private long maxDelayMs = 10000;
        
        // Getters and setters
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public long getInitialDelayMs() { return initialDelayMs; }
        public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }
        
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
        
        public long getMaxDelayMs() { return maxDelayMs; }
        public void setMaxDelayMs(long maxDelayMs) { this.maxDelayMs = maxDelayMs; }
    }
    
    /**
     * Rate limiting configuration settings.
     */
    public static class RateLimitSettings {
        private int requestsPerMinute = 60;
        private int requestsPerHour = 1000;
        private int tokensPerMinute = 10000;
        private boolean enabled = true;
        
        // Getters and setters
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
        
        public int getRequestsPerHour() { return requestsPerHour; }
        public void setRequestsPerHour(int requestsPerHour) { this.requestsPerHour = requestsPerHour; }
        
        public int getTokensPerMinute() { return tokensPerMinute; }
        public void setTokensPerMinute(int tokensPerMinute) { this.tokensPerMinute = tokensPerMinute; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}