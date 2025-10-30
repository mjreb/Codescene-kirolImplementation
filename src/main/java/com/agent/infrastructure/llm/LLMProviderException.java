package com.agent.infrastructure.llm;

/**
 * Exception thrown by LLM providers.
 */
public class LLMProviderException extends RuntimeException {
    
    private final boolean retryable;
    private final String providerId;
    
    public LLMProviderException(String message) {
        this(message, null, false, null);
    }
    
    public LLMProviderException(String message, Throwable cause) {
        this(message, cause, false, null);
    }
    
    public LLMProviderException(String message, boolean retryable) {
        this(message, null, retryable, null);
    }
    
    public LLMProviderException(String message, Throwable cause, boolean retryable, String providerId) {
        super(message, cause);
        this.retryable = retryable;
        this.providerId = providerId;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    public String getProviderId() {
        return providerId;
    }
}