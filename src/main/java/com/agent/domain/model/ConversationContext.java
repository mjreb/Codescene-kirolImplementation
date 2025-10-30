package com.agent.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the context of a conversation including state and metadata.
 */
public class ConversationContext {
    private String conversationId;
    private String agentId;
    private String userId;
    private Map<String, Object> contextData;
    private ReActState reActState;
    private Instant createdAt;
    private Instant lastUpdated;
    
    public ConversationContext() {
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
    }
    
    public ConversationContext(String conversationId, String agentId, String userId) {
        this();
        this.conversationId = conversationId;
        this.agentId = agentId;
        this.userId = userId;
    }
    
    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Map<String, Object> getContextData() { return contextData; }
    public void setContextData(Map<String, Object> contextData) { this.contextData = contextData; }
    
    public ReActState getReActState() { return reActState; }
    public void setReActState(ReActState reActState) { this.reActState = reActState; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}