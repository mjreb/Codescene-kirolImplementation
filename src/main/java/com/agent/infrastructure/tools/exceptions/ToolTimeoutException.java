package com.agent.infrastructure.tools.exceptions;

import java.time.Duration;

/**
 * Exception thrown when tool execution exceeds the configured timeout.
 */
public class ToolTimeoutException extends ToolExecutionException {
    
    private final Duration timeout;
    private final Duration actualDuration;
    
    public ToolTimeoutException(String toolName, Duration timeout, Duration actualDuration) {
        super(toolName, "TIMEOUT_ERROR", 
                String.format("Tool execution timed out after %s (limit: %s)", 
                        formatDuration(actualDuration), formatDuration(timeout)));
        this.timeout = timeout;
        this.actualDuration = actualDuration;
    }
    
    public Duration getTimeout() {
        return timeout;
    }
    
    public Duration getActualDuration() {
        return actualDuration;
    }
    
    private static String formatDuration(Duration duration) {
        if (duration == null) {
            return "unknown";
        }
        
        long seconds = duration.getSeconds();
        long millis = duration.toMillisPart();
        
        if (seconds > 0) {
            return String.format("%d.%03ds", seconds, millis);
        } else {
            return String.format("%dms", duration.toMillis());
        }
    }
    
    @Override
    public String toString() {
        return String.format("ToolTimeoutException[tool=%s, timeout=%s, actual=%s]", 
                getToolName(), formatDuration(timeout), formatDuration(actualDuration));
    }
}