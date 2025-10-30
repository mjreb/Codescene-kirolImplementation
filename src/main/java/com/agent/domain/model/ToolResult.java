package com.agent.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the result of a tool execution.
 */
public class ToolResult {
    private String toolName;
    private boolean success;
    private Object result;
    private String errorMessage;
    private Map<String, Object> metadata;
    private Instant executionTime;
    private long durationMs;
    
    public ToolResult() {
        this.executionTime = Instant.now();
    }
    
    public ToolResult(String toolName, boolean success, Object result) {
        this();
        this.toolName = toolName;
        this.success = success;
        this.result = result;
    }
    
    // Getters and setters
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Instant getExecutionTime() { return executionTime; }
    public void setExecutionTime(Instant executionTime) { this.executionTime = executionTime; }
    
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
}