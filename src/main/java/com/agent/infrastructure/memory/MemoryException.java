package com.agent.infrastructure.memory;

/**
 * Exception thrown when memory operations fail.
 */
public class MemoryException extends RuntimeException {
    
    public MemoryException(String message) {
        super(message);
    }
    
    public MemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}