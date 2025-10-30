package com.agent.infrastructure.tools.exceptions;

/**
 * Exception thrown when tool parameters are invalid or missing.
 */
public class ToolParameterException extends ToolExecutionException {
    
    private final String parameterName;
    
    public ToolParameterException(String toolName, String parameterName, String message) {
        super(toolName, "PARAMETER_ERROR", message);
        this.parameterName = parameterName;
    }
    
    public ToolParameterException(String toolName, String parameterName, String message, Throwable cause) {
        super(toolName, "PARAMETER_ERROR", message, cause);
        this.parameterName = parameterName;
    }
    
    public String getParameterName() {
        return parameterName;
    }
    
    @Override
    public String toString() {
        return String.format("ToolParameterException[tool=%s, parameter=%s, message=%s]", 
                getToolName(), parameterName, getMessage());
    }
}