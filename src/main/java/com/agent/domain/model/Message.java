package com.agent.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a message in a conversation.
 */
public class Message {
    private String id;
    private String conversationId;
    private MessageType type;
    private String content;
    private Map<String, Object> metadata;
    private Instant timestamp;
    
    public Message() {
        this.timestamp = Instant.now();
    }
    
    public Message(String conversationId, MessageType type, String content) {
        this();
        this.conversationId = conversationId;
        this.type = type;
        this.content = content;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public enum MessageType {
        USER, ASSISTANT, SYSTEM, TOOL_CALL, TOOL_RESULT
    }
}