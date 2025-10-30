package com.agent.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a response from an LLM provider.
 */
public class LLMResponse {
    private String content;
    private String model;
    private TokenUsage tokenUsage;
    private Map<String, Object> metadata;
    private Instant timestamp;
    private String providerId;
    
    public LLMResponse() {
        this.timestamp = Instant.now();
    }
    
    public LLMResponse(String content, String model, String providerId) {
        this();
        this.content = content;
        this.model = model;
        this.providerId = providerId;
    }
    
    // Getters and setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public TokenUsage getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(TokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
}