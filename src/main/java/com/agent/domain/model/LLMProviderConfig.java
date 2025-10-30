package com.agent.domain.model;

import java.util.Map;

/**
 * Configuration settings for LLM providers including authentication and retry settings.
 */
public class LLMProviderConfig {
    private String providerId;
    private String providerName;
    private String apiKey;
    private String baseUrl;
    private Map<String, String> headers;
    private RetryConfig retryConfig;
    private RateLimitConfig rateLimitConfig;
    private boolean enabled;
    private int priority;
    private int timeoutSeconds;
    
    public LLMProviderConfig() {
        this.enabled = true;
        this.priority = 1;
        this.timeoutSeconds = 30;
    }
    
    public LLMProviderConfig(String providerId, String providerName, String apiKey) {
        this();
        this.providerId = providerId;
        this.providerName = providerName;
        this.apiKey = apiKey;
    }
    
    // Getters and setters
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    
    public RetryConfig getRetryConfig() { return retryConfig; }
    public void setRetryConfig(RetryConfig retryConfig) { this.retryConfig = retryConfig; }
    
    public RateLimitConfig getRateLimitConfig() { return rateLimitConfig; }
    public void setRateLimitConfig(RateLimitConfig rateLimitConfig) { this.rateLimitConfig = rateLimitConfig; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    /**
     * Configuration for retry behavior.
     */
    public static class RetryConfig {
        private int maxRetries;
        private long initialDelayMs;
        private double backoffMultiplier;
        private long maxDelayMs;
        
        public RetryConfig() {
            this.maxRetries = 3;
            this.initialDelayMs = 1000;
            this.backoffMultiplier = 2.0;
            this.maxDelayMs = 10000;
        }
        
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
     * Configuration for rate limiting.
     */
    public static class RateLimitConfig {
        private int requestsPerMinute;
        private int requestsPerHour;
        private int tokensPerMinute;
        private boolean enabled;
        
        public RateLimitConfig() {
            this.requestsPerMinute = 60;
            this.requestsPerHour = 1000;
            this.tokensPerMinute = 10000;
            this.enabled = true;
        }
        
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