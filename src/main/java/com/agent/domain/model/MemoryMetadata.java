package com.agent.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents metadata associated with stored memory.
 */
public class MemoryMetadata {
    private String source;
    private Instant createdAt;
    private Instant expiresAt;
    private Map<String, Object> tags;
    private int accessCount;
    private Instant lastAccessed;
    
    public MemoryMetadata() {
        this.createdAt = Instant.now();
        this.accessCount = 0;
    }
    
    public MemoryMetadata(String source) {
        this();
        this.source = source;
    }
    
    // Getters and setters
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public Map<String, Object> getTags() { return tags; }
    public void setTags(Map<String, Object> tags) { this.tags = tags; }
    
    public int getAccessCount() { return accessCount; }
    public void setAccessCount(int accessCount) { this.accessCount = accessCount; }
    
    public Instant getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(Instant lastAccessed) { this.lastAccessed = lastAccessed; }
}