package com.agent.infrastructure.tools.exceptions;

import java.util.List;

/**
 * Exception thrown when a tool execution is denied due to insufficient permissions.
 */
public class ToolPermissionException extends ToolExecutionException {
    
    private final List<String> requiredPermissions;
    private final List<String> userPermissions;
    
    public ToolPermissionException(String toolName, List<String> requiredPermissions, List<String> userPermissions) {
        super(toolName, "PERMISSION_DENIED", 
                String.format("Insufficient permissions to execute tool. Required: %s, User has: %s", 
                        requiredPermissions, userPermissions));
        this.requiredPermissions = requiredPermissions;
        this.userPermissions = userPermissions;
    }
    
    public List<String> getRequiredPermissions() {
        return requiredPermissions;
    }
    
    public List<String> getUserPermissions() {
        return userPermissions;
    }
    
    @Override
    public String toString() {
        return String.format("ToolPermissionException[tool=%s, required=%s, user=%s]", 
                getToolName(), requiredPermissions, userPermissions);
    }
}