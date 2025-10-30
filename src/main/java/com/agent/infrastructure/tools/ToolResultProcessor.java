package com.agent.infrastructure.tools;

import com.agent.domain.model.ToolResult;
import com.agent.infrastructure.tools.exceptions.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes and validates tool execution results.
 */
public class ToolResultProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolResultProcessor.class);
    private static final int MAX_RESULT_SIZE = 1024 * 1024; // 1MB
    
    /**
     * Processes a tool result, adding metadata and performing validation.
     * 
     * @param result The raw result from tool execution
     * @param toolName The name of the tool that was executed
     * @param duration The actual execution duration
     * @return The processed result
     */
    public ToolResult processResult(ToolResult result, String toolName, Duration duration) {
        if (result == null) {
            throw new ToolExecutionException(toolName, "NULL_RESULT", "Tool returned null result");
        }
        
        // Ensure basic fields are set
        if (result.getToolName() == null) {
            result.setToolName(toolName);
        }
        
        if (result.getExecutionTime() == null) {
            result.setExecutionTime(Instant.now());
        }
        
        result.setDurationMs(duration.toMillis());
        
        // Add execution metadata
        addExecutionMetadata(result, duration);
        
        // Validate result size
        validateResultSize(result, toolName);
        
        // Sanitize result content
        sanitizeResult(result);
        
        return result;
    }
    
    /**
     * Adds execution metadata to the result.
     */
    private void addExecutionMetadata(ToolResult result, Duration duration) {
        Map<String, Object> metadata = result.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            result.setMetadata(metadata);
        }
        
        metadata.put("execution_duration_ms", duration.toMillis());
        metadata.put("execution_duration_human", formatDuration(duration));
        metadata.put("processed_at", Instant.now().toString());
        
        // Add performance classification
        if (duration.toMillis() < 100) {
            metadata.put("performance", "fast");
        } else if (duration.toMillis() < 1000) {
            metadata.put("performance", "normal");
        } else if (duration.toMillis() < 10000) {
            metadata.put("performance", "slow");
        } else {
            metadata.put("performance", "very_slow");
        }
    }
    
    /**
     * Validates that the result size is within acceptable limits.
     */
    private void validateResultSize(ToolResult result, String toolName) {
        try {
            int resultSize = calculateResultSize(result);
            
            if (resultSize > MAX_RESULT_SIZE) {
                logger.warn("Tool {} returned large result: {} bytes", toolName, resultSize);
                
                // Truncate the result if it's too large
                if (result.getResult() instanceof String) {
                    String originalResult = (String) result.getResult();
                    String truncatedResult = originalResult.substring(0, Math.min(originalResult.length(), MAX_RESULT_SIZE / 2));
                    result.setResult(truncatedResult + "\n... [Result truncated due to size limit]");
                    
                    Map<String, Object> metadata = result.getMetadata();
                    if (metadata == null) {
                        metadata = new HashMap<>();
                        result.setMetadata(metadata);
                    }
                    metadata.put("truncated", true);
                    metadata.put("original_size", resultSize);
                    metadata.put("truncated_size", calculateResultSize(result));
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to validate result size for tool {}: {}", toolName, e.getMessage());
        }
    }
    
    /**
     * Calculates the approximate size of a tool result.
     */
    private int calculateResultSize(ToolResult result) {
        int size = 0;
        
        if (result.getResult() != null) {
            size += result.getResult().toString().length() * 2; // Approximate UTF-16 size
        }
        
        if (result.getErrorMessage() != null) {
            size += result.getErrorMessage().length() * 2;
        }
        
        if (result.getMetadata() != null) {
            size += result.getMetadata().toString().length() * 2;
        }
        
        return size;
    }
    
    /**
     * Sanitizes the result content to remove potentially harmful data.
     */
    private void sanitizeResult(ToolResult result) {
        // Sanitize error messages
        if (result.getErrorMessage() != null) {
            String sanitized = sanitizeString(result.getErrorMessage());
            result.setErrorMessage(sanitized);
        }
        
        // Sanitize string results
        if (result.getResult() instanceof String) {
            String sanitized = sanitizeString((String) result.getResult());
            result.setResult(sanitized);
        }
    }
    
    /**
     * Sanitizes a string by removing or escaping potentially harmful content.
     */
    private String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove control characters except common whitespace
        String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        // Limit line length to prevent extremely long lines
        String[] lines = sanitized.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            if (line.length() > 1000) {
                line = line.substring(0, 1000) + "... [line truncated]";
            }
            result.append(line).append("\n");
        }
        
        return result.toString().trim();
    }
    
    private String formatDuration(Duration duration) {
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
}