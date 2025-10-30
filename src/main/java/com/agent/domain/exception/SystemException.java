package com.agent.domain.exception;

/**
 * Exception for system-level errors (database, network, infrastructure).
 */
public class SystemException extends AgentException {
    
    private final String component;
    private final String operation;
    
    public SystemException(String component, String operation, String message) {
        super("SYSTEM_ERROR", ErrorCategory.SYSTEM, message, true);
        this.component = component;
        this.operation = operation;
    }
    
    public SystemException(String component, String operation, String message, Throwable cause) {
        super("SYSTEM_ERROR", ErrorCategory.SYSTEM, message, cause, true);
        this.component = component;
        this.operation = operation;
    }
    
    public String getComponent() {
        return component;
    }
    
    public String getOperation() {
        return operation;
    }
    
    @Override
    public String getUserMessage() {
        return "A system error occurred. Please try again later.";
    }
    
    /**
     * Database connection exception.
     */
    public static class DatabaseException extends SystemException {
        public DatabaseException(String operation, String message, Throwable cause) {
            super("database", operation, message, cause);
        }
        
        @Override
        public String getUserMessage() {
            return "Database is temporarily unavailable. Please try again later.";
        }
    }
    
    /**
     * Network connectivity exception.
     */
    public static class NetworkException extends SystemException {
        public NetworkException(String operation, String message, Throwable cause) {
            super("network", operation, message, cause);
        }
        
        @Override
        public String getUserMessage() {
            return "Network connectivity issue. Please check your connection and try again.";
        }
    }
    
    /**
     * File system exception.
     */
    public static class FileSystemException extends SystemException {
        private final String filePath;
        
        public FileSystemException(String operation, String filePath, String message, Throwable cause) {
            super("filesystem", operation, message, cause);
            this.filePath = filePath;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        @Override
        public String getUserMessage() {
            return "File system error occurred. Please try again later.";
        }
    }
    
    /**
     * Resource exhaustion exception.
     */
    public static class ResourceExhaustedException extends SystemException {
        private final String resourceType;
        private final long currentUsage;
        private final long maxCapacity;
        
        public ResourceExhaustedException(String resourceType, long currentUsage, long maxCapacity) {
            super("resource", "allocate", 
                    String.format("%s resource exhausted: %d/%d", resourceType, currentUsage, maxCapacity));
            this.resourceType = resourceType;
            this.currentUsage = currentUsage;
            this.maxCapacity = maxCapacity;
        }
        
        public String getResourceType() {
            return resourceType;
        }
        
        public long getCurrentUsage() {
            return currentUsage;
        }
        
        public long getMaxCapacity() {
            return maxCapacity;
        }
        
        @Override
        public String getUserMessage() {
            return "System resources are currently exhausted. Please try again later.";
        }
    }
}