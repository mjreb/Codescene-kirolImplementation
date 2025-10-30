package com.agent.domain.exception;

import java.time.Duration;
import java.util.List;

/**
 * Exception for tool execution related errors.
 */
public class ToolExecutionException extends AgentException {
    
    private final String toolName;
    
    public ToolExecutionException(String toolName, String message) {
        super("TOOL_EXECUTION_ERROR", ErrorCategory.TOOL_EXECUTION, message);
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super("TOOL_EXECUTION_ERROR", ErrorCategory.TOOL_EXECUTION, message, cause);
        this.toolName = toolName;
    }
    
    public ToolExecutionException(String toolName, String message, boolean retryable) {
        super("TOOL_EXECUTION_ERROR", ErrorCategory.TOOL_EXECUTION, message, retryable);
        this.toolName = toolName;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    @Override
    public String getUserMessage() {
        return String.format("Unable to execute tool '%s'. Please try again.", toolName);
    }
    
    /**
     * Tool not found exception.
     */
    public static class ToolNotFoundException extends ToolExecutionException {
        public ToolNotFoundException(String toolName) {
            super(toolName, "Tool not found: " + toolName, false);
        }
        
        @Override
        public String getUserMessage() {
            return String.format("The requested tool '%s' is not available.", getToolName());
        }
    }
    
    /**
     * Tool parameter validation exception.
     */
    public static class ParameterValidationException extends ToolExecutionException {
        private final String parameterName;
        private final String validationError;
        
        public ParameterValidationException(String toolName, String parameterName, String validationError) {
            super(toolName, String.format("Parameter validation failed for '%s': %s", parameterName, validationError), false);
            this.parameterName = parameterName;
            this.validationError = validationError;
        }
        
        public String getParameterName() {
            return parameterName;
        }
        
        public String getValidationError() {
            return validationError;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("Invalid parameter '%s' for tool '%s': %s", parameterName, getToolName(), validationError);
        }
    }
    
    /**
     * Tool permission denied exception.
     */
    public static class PermissionDeniedException extends ToolExecutionException {
        private final List<String> requiredPermissions;
        
        public PermissionDeniedException(String toolName, List<String> requiredPermissions) {
            super(toolName, String.format("Insufficient permissions to execute tool. Required: %s", requiredPermissions), false);
            this.requiredPermissions = requiredPermissions;
        }
        
        public List<String> getRequiredPermissions() {
            return requiredPermissions;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("You don't have permission to use the '%s' tool.", getToolName());
        }
    }
    
    /**
     * Tool timeout exception.
     */
    public static class TimeoutException extends ToolExecutionException {
        private final Duration timeout;
        
        public TimeoutException(String toolName, Duration timeout) {
            super(toolName, String.format("Tool execution timed out after %s", formatDuration(timeout)), true);
            this.timeout = timeout;
        }
        
        public Duration getTimeout() {
            return timeout;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("Tool '%s' took too long to execute. Please try again.", getToolName());
        }
        
        private static String formatDuration(Duration duration) {
            if (duration == null) return "unknown";
            long seconds = duration.getSeconds();
            return seconds > 0 ? seconds + "s" : duration.toMillis() + "ms";
        }
    }
}