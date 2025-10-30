package com.agent.domain.exception;

/**
 * Exception for memory management related errors.
 */
public class MemoryException extends AgentException {
    
    private final String memoryType;
    private final String operation;
    
    public MemoryException(String memoryType, String operation, String message) {
        super("MEMORY_ERROR", ErrorCategory.MEMORY, message, true);
        this.memoryType = memoryType;
        this.operation = operation;
    }
    
    public MemoryException(String memoryType, String operation, String message, Throwable cause) {
        super("MEMORY_ERROR", ErrorCategory.MEMORY, message, cause, true);
        this.memoryType = memoryType;
        this.operation = operation;
    }
    
    public String getMemoryType() {
        return memoryType;
    }
    
    public String getOperation() {
        return operation;
    }
    
    @Override
    public String getUserMessage() {
        return "Unable to access conversation memory. Please try again.";
    }
    
    /**
     * Memory storage capacity exceeded exception.
     */
    public static class CapacityExceededException extends MemoryException {
        private final long currentSize;
        private final long maxSize;
        
        public CapacityExceededException(String memoryType, long currentSize, long maxSize) {
            super(memoryType, "store", 
                    String.format("Memory capacity exceeded: %d/%d bytes", currentSize, maxSize));
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }
        
        public long getCurrentSize() {
            return currentSize;
        }
        
        public long getMaxSize() {
            return maxSize;
        }
        
        @Override
        public String getUserMessage() {
            return "Memory storage is full. Please start a new conversation.";
        }
    }
    
    /**
     * Memory corruption or inconsistency exception.
     */
    public static class CorruptionException extends MemoryException {
        public CorruptionException(String memoryType, String details) {
            super(memoryType, "validate", "Memory corruption detected: " + details);
        }
        
        @Override
        public String getUserMessage() {
            return "Conversation data is corrupted. Please start a new conversation.";
        }
    }
    
    /**
     * Memory access timeout exception.
     */
    public static class TimeoutException extends MemoryException {
        public TimeoutException(String memoryType, String operation) {
            super(memoryType, operation, "Memory operation timed out");
        }
        
        @Override
        public String getUserMessage() {
            return "Memory access is taking too long. Please try again.";
        }
    }
}