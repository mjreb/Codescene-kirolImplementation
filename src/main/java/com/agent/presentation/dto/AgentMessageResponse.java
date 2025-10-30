package com.agent.presentation.dto;

import com.agent.domain.model.AgentResponse;
import com.agent.domain.model.TokenUsage;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for agent message responses.
 */
public class AgentMessageResponse {
    
    private String conversationId;
    private String content;
    private AgentResponse.ResponseType type;
    private Map<String, Object> metadata;
    private TokenUsage tokenUsage;
    private Instant timestamp;
    
    public AgentMessageResponse() {}
    
    public AgentMessageResponse(String conversationId, String content, 
                               AgentResponse.ResponseType type, Instant timestamp) {
        this.conversationId = conversationId;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public AgentResponse.ResponseType getType() { return type; }
    public void setType(AgentResponse.ResponseType type) { this.type = type; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public TokenUsage getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(TokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}