package com.agent.presentation.dto;

import com.agent.domain.model.Message;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for message information.
 */
public class MessageResponse {
    
    private String id;
    private Message.MessageType type;
    private String content;
    private Map<String, Object> metadata;
    private Instant timestamp;
    
    public MessageResponse() {}
    
    public MessageResponse(String id, Message.MessageType type, String content, Instant timestamp) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Message.MessageType getType() { return type; }
    public void setType(Message.MessageType type) { this.type = type; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}