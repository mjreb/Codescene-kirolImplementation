package com.agent.infrastructure.tools;

import com.agent.domain.interfaces.ToolFramework;
import com.agent.domain.model.Tool;
import com.agent.domain.model.ToolDefinition;
import com.agent.domain.model.ToolResult;
import com.agent.domain.model.ParameterDefinition;
import com.agent.infrastructure.tools.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Implementation of the ToolFramework interface that manages tool registration,
 * discovery, and execution with parameter validation and type conversion.
 */
@Component
public class ToolFrameworkImpl implements ToolFramework {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolFrameworkImpl.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration MAX_TIMEOUT = Duration.ofMinutes(30);
    
    private final Map<String, Tool> registeredTools = new ConcurrentHashMap<>();
    private final Map<String, Duration> toolTimeouts = new ConcurrentHashMap<>();
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();
    private final ParameterValidator parameterValidator = new ParameterValidator();
    private final ToolResultProcessor resultProcessor = new ToolResultProcessor();
    
    @Override
    public void registerTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        
        ToolDefinition definition = tool.getDefinition();
        if (definition == null || definition.getName() == null || definition.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tool must have a valid definition with a name");
        }
        
        String toolName = definition.getName();
        registeredTools.put(toolName, tool);
        
        // Set default timeout for the tool
        toolTimeouts.put(toolName, DEFAULT_TIMEOUT);
        
        logger.info("Registered tool: {} (timeout: {})", toolName, DEFAULT_TIMEOUT);
    }
    
    /**
     * Register a tool with a custom timeout.
     */
    public void registerTool(Tool tool, Duration timeout) {
        registerTool(tool);
        
        if (timeout != null && timeout.compareTo(MAX_TIMEOUT) <= 0) {
            String toolName = tool.getDefinition().getName();
            toolTimeouts.put(toolName, timeout);
            logger.info("Set custom timeout for tool {}: {}", toolName, timeout);
        }
    }
    
    @Override
    public ToolResult executeTool(String toolName, Map<String, Object> parameters) {
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new ToolExecutionException(toolName, "INVALID_TOOL_NAME", "Tool name cannot be null or empty");
        }
        
        Tool tool = registeredTools.get(toolName);
        if (tool == null) {
            throw new ToolNotFoundException(toolName);
        }
        
        Duration timeout = toolTimeouts.getOrDefault(toolName, DEFAULT_TIMEOUT);
        
        try {
            // Validate and convert parameters
            Map<String, Object> validatedParams = validateAndConvertParameters(tool, parameters);
            
            // Execute the tool with timeout
            Instant startTime = Instant.now();
            ToolResult result = executeWithTimeout(tool, validatedParams, timeout);
            Duration actualDuration = Duration.between(startTime, Instant.now());
            
            // Process and validate the result
            result = resultProcessor.processResult(result, toolName, actualDuration);
            
            logger.debug("Tool {} executed successfully in {}", toolName, formatDuration(actualDuration));
            return result;
            
        } catch (ToolExecutionException e) {
            logger.error("Tool execution error for {}: {}", toolName, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error executing tool {}: {}", toolName, e.getMessage(), e);
            throw new ToolExecutionException(toolName, "UNEXPECTED_ERROR", "Tool execution failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ToolDefinition> getAvailableTools() {
        return registeredTools.values().stream()
                .map(Tool::getDefinition)
                .sorted(Comparator.comparing(ToolDefinition::getName))
                .toList();
    }
    
    @Override
    public CompletableFuture<ToolResult> executeToolAsync(String toolName, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeTool(toolName, parameters);
            } catch (ToolExecutionException e) {
                // Convert exception to error result for async execution
                return createErrorResult(toolName, e.getErrorCode(), e.getMessage());
            }
        }, asyncExecutor);
    }
    
    /**
     * Executes a tool with timeout handling.
     */
    private ToolResult executeWithTimeout(Tool tool, Map<String, Object> parameters, Duration timeout) {
        String toolName = tool.getDefinition().getName();
        
        CompletableFuture<ToolResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                return tool.execute(parameters);
            } catch (Exception e) {
                throw new ToolExecutionException(toolName, "EXECUTION_ERROR", "Tool execution failed", e);
            }
        }, asyncExecutor);
        
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ToolTimeoutException(toolName, timeout, timeout);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ToolExecutionException) {
                throw (ToolExecutionException) cause;
            } else {
                throw new ToolExecutionException(toolName, "EXECUTION_ERROR", "Tool execution failed", cause);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolExecutionException(toolName, "INTERRUPTED", "Tool execution was interrupted", e);
        }
    }
    
    /**
     * Validates and converts parameters according to the tool's parameter definitions.
     */
    private Map<String, Object> validateAndConvertParameters(Tool tool, Map<String, Object> parameters) {
        ToolDefinition definition = tool.getDefinition();
        Map<String, ParameterDefinition> paramDefs = definition.getParameters();
        String toolName = definition.getName();
        
        if (paramDefs == null) {
            paramDefs = Collections.emptyMap();
        }
        
        Map<String, Object> validatedParams = new HashMap<>();
        
        try {
            // Check required parameters
            for (Map.Entry<String, ParameterDefinition> entry : paramDefs.entrySet()) {
                String paramName = entry.getKey();
                ParameterDefinition paramDef = entry.getValue();
                
                Object value = parameters != null ? parameters.get(paramName) : null;
                
                if (paramDef.isRequired() && value == null) {
                    throw new ToolParameterException(toolName, paramName, "Required parameter missing: " + paramName);
                }
                
                if (value == null && paramDef.getDefaultValue() != null) {
                    value = paramDef.getDefaultValue();
                }
                
                if (value != null) {
                    try {
                        // Validate and convert the parameter
                        Object convertedValue = parameterValidator.validateAndConvert(paramDef, value);
                        validatedParams.put(paramName, convertedValue);
                    } catch (IllegalArgumentException e) {
                        throw new ToolParameterException(toolName, paramName, e.getMessage(), e);
                    }
                }
            }
            
            // Check for unexpected parameters
            if (parameters != null) {
                for (String paramName : parameters.keySet()) {
                    if (!paramDefs.containsKey(paramName)) {
                        logger.warn("Unexpected parameter '{}' for tool '{}'", paramName, toolName);
                    }
                }
            }
            
            return validatedParams;
            
        } catch (ToolParameterException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolParameterException(toolName, "unknown", "Parameter validation failed: " + e.getMessage(), e);
        }
    }
    
    private ToolResult createErrorResult(String toolName, String errorMessage) {
        ToolResult result = new ToolResult();
        result.setToolName(toolName);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    private ToolResult createErrorResult(String toolName, String errorCode, String errorMessage) {
        ToolResult result = new ToolResult();
        result.setToolName(toolName);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("error_code", errorCode);
        result.setMetadata(metadata);
        
        return result;
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
    
    /**
     * Clean up resources when the framework is destroyed.
     */
    public void destroy() {
        asyncExecutor.shutdown();
    }
}