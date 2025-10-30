package com.agent.domain.exception;

/**
 * Base exception for all agent-related errors.
 * Provides common error handling functionality and categorization.
 */
public abstract class AgentException extends RuntimeException {
    
    private final String errorCode;
    private final ErrorCategory category;
    private final boolean retryable;
    
    protected AgentException(String errorCode, ErrorCategory category, String message) {
        this(errorCode, category, message, null, false);
    }
    
    protected AgentException(String errorCode, ErrorCategory category, String message, boolean retryable) {
        this(errorCode, category, message, null, retryable);
    }
    
    protected AgentException(String errorCode, ErrorCategory category, String message, Throwable cause) {
        this(errorCode, category, message, cause, false);
    }
    
    protected AgentException(String errorCode, ErrorCategory category, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.category = category;
        this.retryable = retryable;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public ErrorCategory getCategory() {
        return category;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    /**
     * Get user-friendly error message that can be safely displayed to end users.
     */
    public String getUserMessage() {
        return getMessage();
    }
    
    /**
     * Get technical details for logging and debugging.
     */
    public String getTechnicalDetails() {
        return String.format("Error[code=%s, category=%s, retryable=%s]: %s", 
                errorCode, category, retryable, getMessage());
    }
    
    @Override
    public String toString() {
        return String.format("%s[code=%s, category=%s, retryable=%s, message=%s]", 
                getClass().getSimpleName(), errorCode, category, retryable, getMessage());
    }
}