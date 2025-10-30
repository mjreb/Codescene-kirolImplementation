package com.agent.domain.exception;

/**
 * Exception for LLM provider related errors.
 */
public class LLMProviderException extends AgentException {
    
    private final String providerId;
    private final int httpStatusCode;
    
    public LLMProviderException(String providerId, String message) {
        super("LLM_PROVIDER_ERROR", ErrorCategory.LLM_PROVIDER, message, true);
        this.providerId = providerId;
        this.httpStatusCode = 0;
    }
    
    public LLMProviderException(String providerId, String message, Throwable cause) {
        super("LLM_PROVIDER_ERROR", ErrorCategory.LLM_PROVIDER, message, cause, true);
        this.providerId = providerId;
        this.httpStatusCode = 0;
    }
    
    public LLMProviderException(String providerId, String message, int httpStatusCode, boolean retryable) {
        super("LLM_PROVIDER_ERROR", ErrorCategory.LLM_PROVIDER, message, retryable);
        this.providerId = providerId;
        this.httpStatusCode = httpStatusCode;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public int getHttpStatusCode() {
        return httpStatusCode;
    }
    
    @Override
    public String getUserMessage() {
        if (httpStatusCode == 429) {
            return "The AI service is currently busy. Please try again in a moment.";
        } else if (httpStatusCode >= 500) {
            return "The AI service is temporarily unavailable. Please try again later.";
        } else if (httpStatusCode == 401 || httpStatusCode == 403) {
            return "Authentication error with AI service. Please contact support.";
        } else {
            return "Unable to process request with AI service. Please try again.";
        }
    }
    
    /**
     * Rate limit exceeded exception.
     */
    public static class RateLimitExceededException extends LLMProviderException {
        private final long retryAfterSeconds;
        
        public RateLimitExceededException(String providerId, long retryAfterSeconds) {
            super(providerId, "Rate limit exceeded", 429, true);
            this.retryAfterSeconds = retryAfterSeconds;
        }
        
        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("Rate limit exceeded. Please try again in %d seconds.", retryAfterSeconds);
        }
    }
    
    /**
     * Authentication failure exception.
     */
    public static class AuthenticationException extends LLMProviderException {
        public AuthenticationException(String providerId, String message) {
            super(providerId, message, 401, false);
        }
        
        @Override
        public String getUserMessage() {
            return "Authentication failed with AI service. Please contact support.";
        }
    }
    
    /**
     * Service unavailable exception.
     */
    public static class ServiceUnavailableException extends LLMProviderException {
        public ServiceUnavailableException(String providerId, String message) {
            super(providerId, message, 503, true);
        }
        
        @Override
        public String getUserMessage() {
            return "AI service is temporarily unavailable. Please try again later.";
        }
    }
}