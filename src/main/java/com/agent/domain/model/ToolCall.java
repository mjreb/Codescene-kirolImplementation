package com.agent.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a call to a tool with parameters.
 */
public class ToolCall {
    private String toolName;
    private Map<String, Object> parameters;
    private Instant timestamp;
    private String callId;
    
    public ToolCall() {
        this.timestamp = Instant.now();
    }
    
    public ToolCall(String toolName, Map<String, Object> parameters) {
        this();
        this.toolName = toolName;
        this.parameters = parameters;
    }
    
    // Getters and setters
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }
}