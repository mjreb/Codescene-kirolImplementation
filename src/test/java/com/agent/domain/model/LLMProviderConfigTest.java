package com.agent.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LLMProviderConfigTest {
    
    private LLMProviderConfig config;
    private LLMProviderConfig.RetryConfig retryConfig;
    private LLMProviderConfig.RateLimitConfig rateLimitConfig;
    
    @BeforeEach
    void setUp() {
        config = new LLMProviderConfig();
        retryConfig = new LLMProviderConfig.RetryConfig();
        rateLimitConfig = new LLMProviderConfig.RateLimitConfig();
    }
    
    @Test
    void testLLMProviderConfigCreation() {
        LLMProviderConfig newConfig = new LLMProviderConfig("openai", "OpenAI", "sk-test-key");
        
        assertEquals("openai", newConfig.getProviderId());
        assertEquals("OpenAI", newConfig.getProviderName());
        assertEquals("sk-test-key", newConfig.getApiKey());
        assertTrue(newConfig.isEnabled());
        assertEquals(1, newConfig.getPriority());
        assertEquals(30, newConfig.getTimeoutSeconds());
    }
    
    @Test
    void testDefaultConstructor() {
        LLMProviderConfig defaultConfig = new LLMProviderConfig();
        
        assertTrue(defaultConfig.isEnabled());
        assertEquals(1, defaultConfig.getPriority());
        assertEquals(30, defaultConfig.getTimeoutSeconds());
    }
    
    @Test
    void testHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("Content-Type", "application/json");
        
        config.setHeaders(headers);
        
        assertEquals(headers, config.getHeaders());
        assertEquals("Bearer token", config.getHeaders().get("Authorization"));
    }
    
    @Test
    void testRetryConfig() {
        config.setRetryConfig(retryConfig);
        
        assertEquals(retryConfig, config.getRetryConfig());
        assertEquals(3, retryConfig.getMaxRetries());
        assertEquals(1000, retryConfig.getInitialDelayMs());
        assertEquals(2.0, retryConfig.getBackoffMultiplier());
        assertEquals(10000, retryConfig.getMaxDelayMs());
    }
    
    @Test
    void testRateLimitConfig() {
        config.setRateLimitConfig(rateLimitConfig);
        
        assertEquals(rateLimitConfig, config.getRateLimitConfig());
        assertEquals(60, rateLimitConfig.getRequestsPerMinute());
        assertEquals(1000, rateLimitConfig.getRequestsPerHour());
        assertEquals(10000, rateLimitConfig.getTokensPerMinute());
        assertTrue(rateLimitConfig.isEnabled());
    }
    
    @Test
    void testRetryConfigCustomization() {
        retryConfig.setMaxRetries(5);
        retryConfig.setInitialDelayMs(2000);
        retryConfig.setBackoffMultiplier(1.5);
        retryConfig.setMaxDelayMs(30000);
        
        assertEquals(5, retryConfig.getMaxRetries());
        assertEquals(2000, retryConfig.getInitialDelayMs());
        assertEquals(1.5, retryConfig.getBackoffMultiplier());
        assertEquals(30000, retryConfig.getMaxDelayMs());
    }
    
    @Test
    void testRateLimitConfigCustomization() {
        rateLimitConfig.setRequestsPerMinute(120);
        rateLimitConfig.setRequestsPerHour(5000);
        rateLimitConfig.setTokensPerMinute(50000);
        rateLimitConfig.setEnabled(false);
        
        assertEquals(120, rateLimitConfig.getRequestsPerMinute());
        assertEquals(5000, rateLimitConfig.getRequestsPerHour());
        assertEquals(50000, rateLimitConfig.getTokensPerMinute());
        assertFalse(rateLimitConfig.isEnabled());
    }
    
    @Test
    void testConfigurationProperties() {
        config.setProviderId("anthropic");
        config.setProviderName("Anthropic");
        config.setApiKey("ant-key");
        config.setBaseUrl("https://api.anthropic.com");
        config.setEnabled(false);
        config.setPriority(2);
        config.setTimeoutSeconds(60);
        
        assertEquals("anthropic", config.getProviderId());
        assertEquals("Anthropic", config.getProviderName());
        assertEquals("ant-key", config.getApiKey());
        assertEquals("https://api.anthropic.com", config.getBaseUrl());
        assertFalse(config.isEnabled());
        assertEquals(2, config.getPriority());
        assertEquals(60, config.getTimeoutSeconds());
    }
}