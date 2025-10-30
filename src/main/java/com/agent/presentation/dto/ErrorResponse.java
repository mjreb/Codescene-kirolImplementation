package com.agent.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard error response DTO for API error handling.
 * Provides comprehensive error information with user-friendly messages.
 */
public class ErrorResponse {
    
    private String error;
    private String message;
    private int status;
    private String path;
    private Instant timestamp;
    private List<String> details;
    private String errorCode;
    private String category;
    private boolean retryable;
    private Map<String, Object> metadata;
    
    public ErrorResponse() {
        this.timestamp = Instant.now();
    }
    
    public ErrorResponse(String error, String message, int status, String path) {
        this();
        this.error = error;
        this.message = message;
        this.status = status;
        this.path = path;
    }
    
    // Getters and setters
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}