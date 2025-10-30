package com.agent.presentation.dto;

import com.agent.domain.model.ConversationState;
import com.agent.domain.model.TokenUsage;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for conversation information.
 */
public class ConversationResponse {
    
    private String id;
    private String agentId;
    private String userId;
    private ConversationState.ConversationStatus status;
    private String title;
    private int messageCount;
    private List<MessageResponse> messages;
    private TokenUsage tokenUsage;
    private Instant createdAt;
    private Instant lastActivity;
    
    public ConversationResponse() {}
    
    public ConversationResponse(String id, String agentId, String userId, 
                               ConversationState.ConversationStatus status) {
        this.id = id;
        this.agentId = agentId;
        this.userId = userId;
        this.status = status;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public ConversationState.ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationState.ConversationStatus status) { this.status = status; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
    
    public List<MessageResponse> getMessages() { return messages; }
    public void setMessages(List<MessageResponse> messages) { this.messages = messages; }
    
    public TokenUsage getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(TokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastActivity() { return lastActivity; }
    public void setLastActivity(Instant lastActivity) { this.lastActivity = lastActivity; }
}