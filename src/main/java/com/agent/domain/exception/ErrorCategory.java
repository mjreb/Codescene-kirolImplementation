package com.agent.domain.exception;

/**
 * Categories of errors that can occur in the agent system.
 * Used for error classification and handling strategies.
 */
public enum ErrorCategory {
    
    /**
     * LLM provider related errors (API failures, rate limits, authentication).
     */
    LLM_PROVIDER("LLM Provider Error"),
    
    /**
     * Tool execution errors (parameter validation, execution failures, timeouts).
     */
    TOOL_EXECUTION("Tool Execution Error"),
    
    /**
     * Memory management errors (storage failures, retrieval errors, capacity limits).
     */
    MEMORY("Memory Error"),
    
    /**
     * Token limit and budget errors (quota exceeded, rate limits).
     */
    TOKEN_LIMIT("Token Limit Error"),
    
    /**
     * Configuration and setup errors (invalid settings, missing credentials).
     */
    CONFIGURATION("Configuration Error"),
    
    /**
     * Authentication and authorization errors.
     */
    SECURITY("Security Error"),
    
    /**
     * Input validation and request processing errors.
     */
    VALIDATION("Validation Error"),
    
    /**
     * Business logic and domain rule violations.
     */
    BUSINESS_LOGIC("Business Logic Error"),
    
    /**
     * External service integration errors.
     */
    EXTERNAL_SERVICE("External Service Error"),
    
    /**
     * System-level errors (database, network, infrastructure).
     */
    SYSTEM("System Error");
    
    private final String displayName;
    
    ErrorCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}