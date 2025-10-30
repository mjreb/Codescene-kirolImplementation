package com.agent.domain.exception;

/**
 * Exception for external service integration errors.
 */
public class ExternalServiceException extends AgentException {
    
    private final String serviceName;
    private final String serviceUrl;
    private final int httpStatusCode;
    
    public ExternalServiceException(String serviceName, String message) {
        super("EXTERNAL_SERVICE_ERROR", ErrorCategory.EXTERNAL_SERVICE, message, true);
        this.serviceName = serviceName;
        this.serviceUrl = null;
        this.httpStatusCode = 0;
    }
    
    public ExternalServiceException(String serviceName, String serviceUrl, String message, int httpStatusCode) {
        super("EXTERNAL_SERVICE_ERROR", ErrorCategory.EXTERNAL_SERVICE, message, httpStatusCode >= 500);
        this.serviceName = serviceName;
        this.serviceUrl = serviceUrl;
        this.httpStatusCode = httpStatusCode;
    }
    
    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_ERROR", ErrorCategory.EXTERNAL_SERVICE, message, cause, true);
        this.serviceName = serviceName;
        this.serviceUrl = null;
        this.httpStatusCode = 0;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getServiceUrl() {
        return serviceUrl;
    }
    
    public int getHttpStatusCode() {
        return httpStatusCode;
    }
    
    @Override
    public String getUserMessage() {
        if (httpStatusCode >= 500) {
            return String.format("The %s service is temporarily unavailable. Please try again later.", serviceName);
        } else if (httpStatusCode == 429) {
            return String.format("The %s service is currently busy. Please try again in a moment.", serviceName);
        } else {
            return String.format("Unable to connect to %s service. Please try again.", serviceName);
        }
    }
    
    /**
     * Service timeout exception.
     */
    public static class TimeoutException extends ExternalServiceException {
        private final long timeoutMillis;
        
        public TimeoutException(String serviceName, long timeoutMillis) {
            super(serviceName, String.format("Service call timed out after %dms", timeoutMillis));
            this.timeoutMillis = timeoutMillis;
        }
        
        public long getTimeoutMillis() {
            return timeoutMillis;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("The %s service is taking too long to respond. Please try again.", getServiceName());
        }
    }
    
    /**
     * Service unavailable exception.
     */
    public static class ServiceUnavailableException extends ExternalServiceException {
        public ServiceUnavailableException(String serviceName) {
            super(serviceName, null, "Service is unavailable", 503);
        }
        
        @Override
        public String getUserMessage() {
            return String.format("The %s service is currently unavailable. Please try again later.", getServiceName());
        }
    }
    
    /**
     * Service authentication exception.
     */
    public static class AuthenticationException extends ExternalServiceException {
        public AuthenticationException(String serviceName, String message) {
            super(serviceName, null, message, 401);
        }
        
        @Override
        public String getUserMessage() {
            return String.format("Authentication failed with %s service. Please contact support.", getServiceName());
        }
    }
}