package com.agent.infrastructure.tools.exceptions;

/**
 * Exception thrown when a requested tool is not found in the registry.
 */
public class ToolNotFoundException extends ToolExecutionException {
    
    public ToolNotFoundException(String toolName) {
        super(toolName, "TOOL_NOT_FOUND", "Tool not found: " + toolName);
    }
    
    public ToolNotFoundException(String toolName, String message) {
        super(toolName, "TOOL_NOT_FOUND", message);
    }
    
    @Override
    public String toString() {
        return String.format("ToolNotFoundException[tool=%s, message=%s]", 
                getToolName(), getMessage());
    }
}