package com.agent.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents an intelligent agent with configuration and tool associations.
 */
public class Agent {
    private String id;
    private String name;
    private String description;
    private AgentConfiguration configuration;
    private List<String> availableTools;
    private MemoryConfiguration memoryConfig;
    private TokenLimits tokenLimits;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean active;
    
    public Agent() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.active = true;
    }
    
    public Agent(String id, String name, String description) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public AgentConfiguration getConfiguration() { return configuration; }
    public void setConfiguration(AgentConfiguration configuration) { 
        this.configuration = configuration;
        this.updatedAt = Instant.now();
    }
    
    public List<String> getAvailableTools() { return availableTools; }
    public void setAvailableTools(List<String> availableTools) { 
        this.availableTools = availableTools;
        this.updatedAt = Instant.now();
    }
    
    public MemoryConfiguration getMemoryConfig() { return memoryConfig; }
    public void setMemoryConfig(MemoryConfiguration memoryConfig) { 
        this.memoryConfig = memoryConfig;
        this.updatedAt = Instant.now();
    }
    
    public TokenLimits getTokenLimits() { return tokenLimits; }
    public void setTokenLimits(TokenLimits tokenLimits) { 
        this.tokenLimits = tokenLimits;
        this.updatedAt = Instant.now();
    }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { 
        this.active = active;
        this.updatedAt = Instant.now();
    }
}