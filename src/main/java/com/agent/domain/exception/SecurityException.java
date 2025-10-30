package com.agent.domain.exception;

/**
 * Exception for security and authorization related errors.
 */
public class SecurityException extends AgentException {
    
    private final String userId;
    private final String resource;
    private final String action;
    
    public SecurityException(String message) {
        super("SECURITY_ERROR", ErrorCategory.SECURITY, message, false);
        this.userId = null;
        this.resource = null;
        this.action = null;
    }
    
    public SecurityException(String userId, String resource, String action, String message) {
        super("SECURITY_ERROR", ErrorCategory.SECURITY, message, false);
        this.userId = userId;
        this.resource = resource;
        this.action = action;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getAction() {
        return action;
    }
    
    @Override
    public String getUserMessage() {
        return "Access denied. You don't have permission to perform this action.";
    }
    
    /**
     * Authentication failure exception.
     */
    public static class AuthenticationException extends SecurityException {
        public AuthenticationException(String message) {
            super(message);
        }
        
        @Override
        public String getUserMessage() {
            return "Authentication failed. Please log in again.";
        }
    }
    
    /**
     * Authorization failure exception.
     */
    public static class AuthorizationException extends SecurityException {
        public AuthorizationException(String userId, String resource, String action) {
            super(userId, resource, action, 
                    String.format("User '%s' not authorized to '%s' on resource '%s'", userId, action, resource));
        }
        
        @Override
        public String getUserMessage() {
            return String.format("You don't have permission to access %s.", getResource());
        }
    }
    
    /**
     * Invalid token exception.
     */
    public static class InvalidTokenException extends SecurityException {
        public InvalidTokenException(String message) {
            super(message);
        }
        
        @Override
        public String getUserMessage() {
            return "Invalid or expired authentication token. Please log in again.";
        }
    }
    
    /**
     * Rate limit exceeded exception.
     */
    public static class RateLimitExceededException extends SecurityException {
        private final long retryAfterSeconds;
        
        public RateLimitExceededException(String userId, long retryAfterSeconds) {
            super(userId, null, null, "Rate limit exceeded");
            this.retryAfterSeconds = retryAfterSeconds;
        }
        
        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("Too many requests. Please try again in %d seconds.", retryAfterSeconds);
        }
    }
}