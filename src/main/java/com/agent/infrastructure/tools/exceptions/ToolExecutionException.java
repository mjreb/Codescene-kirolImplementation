package com.agent.infrastructure.tools.exceptions;

/**
 * Base exception for tool execution errors.
 */
public class ToolExecutionException extends RuntimeException {
    
    private final String toolName;
    private final String errorCode;
    
    public ToolExecutionException(String toolName, String message) {
        super(message);
        this.toolName = toolName;
        this.errorCode = "TOOL_EXECUTION_ERROR";
    }
    
    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super(message, cause);
        this.toolName = toolName;
        this.errorCode = "TOOL_EXECUTION_ERROR";
    }
    
    public ToolExecutionException(String toolName, String errorCode, String message) {
        super(message);
        this.toolName = toolName;
        this.errorCode = errorCode;
    }
    
    public ToolExecutionException(String toolName, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.toolName = toolName;
        this.errorCode = errorCode;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String toString() {
        return String.format("ToolExecutionException[tool=%s, code=%s, message=%s]", 
                toolName, errorCode, getMessage());
    }
}