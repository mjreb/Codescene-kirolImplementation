package com.agent.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for sending a message to an existing conversation.
 */
public class SendMessageRequest {
    
    @NotBlank(message = "Message content is required")
    @Size(max = 10000, message = "Message cannot exceed 10000 characters")
    private String content;
    
    private boolean streaming = false;
    
    public SendMessageRequest() {}
    
    public SendMessageRequest(String content) {
        this.content = content;
    }
    
    public SendMessageRequest(String content, boolean streaming) {
        this.content = content;
        this.streaming = streaming;
    }
    
    // Getters and setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public boolean isStreaming() { return streaming; }
    public void setStreaming(boolean streaming) { this.streaming = streaming; }
}