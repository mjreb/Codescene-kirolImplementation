package com.agent.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents the current state of a conversation.
 */
public class ConversationState {
    private String conversationId;
    private ConversationStatus status;
    private ReActPhase currentPhase;
    private List<Message> messages;
    private ConversationContext context;
    private Instant lastActivity;
    
    public ConversationState() {}
    
    public ConversationState(String conversationId, ConversationStatus status) {
        this.conversationId = conversationId;
        this.status = status;
        this.lastActivity = Instant.now();
    }
    
    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }
    
    public ReActPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(ReActPhase currentPhase) { this.currentPhase = currentPhase; }
    
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    
    public ConversationContext getContext() { return context; }
    public void setContext(ConversationContext context) { this.context = context; }
    
    public Instant getLastActivity() { return lastActivity; }
    public void setLastActivity(Instant lastActivity) { this.lastActivity = lastActivity; }
    
    public enum ConversationStatus {
        ACTIVE, PAUSED, COMPLETED, ERROR
    }
    

}