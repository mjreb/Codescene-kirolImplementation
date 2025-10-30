package com.agent.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new conversation.
 */
public class CreateConversationRequest {
    
    @NotBlank(message = "Agent ID is required")
    private String agentId;
    
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;
    
    @NotBlank(message = "Initial message is required")
    @Size(max = 10000, message = "Initial message cannot exceed 10000 characters")
    private String initialMessage;
    
    public CreateConversationRequest() {}
    
    public CreateConversationRequest(String agentId, String title, String initialMessage) {
        this.agentId = agentId;
        this.title = title;
        this.initialMessage = initialMessage;
    }
    
    // Getters and setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getInitialMessage() { return initialMessage; }
    public void setInitialMessage(String initialMessage) { this.initialMessage = initialMessage; }
}