package com.agent.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a conversation between a user and an agent with state management and message history.
 */
public class Conversation {
    private String id;
    private String agentId;
    private String userId;
    private ConversationState.ConversationStatus status;
    private List<Message> messages;
    private ConversationContext context;
    private TokenUsage tokenUsage;
    private Instant createdAt;
    private Instant lastActivity;
    private String title;
    private int messageCount;
    
    public Conversation() {
        this.createdAt = Instant.now();
        this.lastActivity = Instant.now();
        this.status = ConversationState.ConversationStatus.ACTIVE;
        this.messages = new ArrayList<>();
        this.messageCount = 0;
    }
    
    public Conversation(String id, String agentId, String userId) {
        this();
        this.id = id;
        this.agentId = agentId;
        this.userId = userId;
    }
    
    /**
     * Adds a message to the conversation and updates activity timestamp.
     */
    public void addMessage(Message message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
        this.messageCount = this.messages.size();
        this.lastActivity = Instant.now();
        
        // Set conversation ID on message if not already set
        if (message.getConversationId() == null) {
            message.setConversationId(this.id);
        }
    }
    
    /**
     * Updates the conversation status and last activity timestamp.
     */
    public void updateStatus(ConversationState.ConversationStatus newStatus) {
        this.status = newStatus;
        this.lastActivity = Instant.now();
    }
    
    /**
     * Checks if the conversation is active.
     */
    public boolean isActive() {
        return this.status == ConversationState.ConversationStatus.ACTIVE;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public ConversationState.ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationState.ConversationStatus status) { 
        this.status = status;
        this.lastActivity = Instant.now();
    }
    
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { 
        this.messages = messages;
        this.messageCount = messages != null ? messages.size() : 0;
    }
    
    public ConversationContext getContext() { return context; }
    public void setContext(ConversationContext context) { this.context = context; }
    
    public TokenUsage getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(TokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastActivity() { return lastActivity; }
    public void setLastActivity(Instant lastActivity) { this.lastActivity = lastActivity; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
}