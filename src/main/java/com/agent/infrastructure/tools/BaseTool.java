package com.agent.infrastructure.tools;

import com.agent.domain.model.Tool;
import com.agent.domain.model.ToolDefinition;
import com.agent.domain.model.ToolResult;
import com.agent.domain.model.ParameterDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Abstract base class for tool implementations that provides common functionality
 * and parameter validation.
 */
public abstract class BaseTool implements Tool {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final ToolDefinition definition;
    
    protected BaseTool(ToolDefinition definition) {
        this.definition = definition;
    }
    
    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            logger.debug("Executing tool '{}' with parameters: {}", definition.getName(), parameters);
            
            // Validate parameters using the tool's own validation
            if (!validateParameters(parameters)) {
                return createErrorResult("Parameter validation failed");
            }
            
            // Execute the tool-specific logic
            ToolResult result = executeInternal(parameters);
            
            if (result == null) {
                result = createErrorResult("Tool returned null result");
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing tool '{}': {}", definition.getName(), e.getMessage(), e);
            return createErrorResult("Tool execution failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (definition.getParameters() == null) {
            return true; // No parameters defined, so validation passes
        }
        
        // Check required parameters
        for (Map.Entry<String, ParameterDefinition> entry : definition.getParameters().entrySet()) {
            String paramName = entry.getKey();
            ParameterDefinition paramDef = entry.getValue();
            
            if (paramDef.isRequired()) {
                Object value = parameters != null ? parameters.get(paramName) : null;
                if (value == null) {
                    logger.warn("Required parameter '{}' is missing for tool '{}'", paramName, definition.getName());
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Template method for tool-specific execution logic.
     * Subclasses must implement this method to provide their functionality.
     * 
     * @param parameters The validated parameters
     * @return The result of the tool execution
     */
    protected abstract ToolResult executeInternal(Map<String, Object> parameters);
    
    /**
     * Helper method to create a successful result.
     */
    protected ToolResult createSuccessResult(Object result) {
        return new ToolResult(definition.getName(), true, result);
    }
    
    /**
     * Helper method to create an error result.
     */
    protected ToolResult createErrorResult(String errorMessage) {
        ToolResult result = new ToolResult();
        result.setToolName(definition.getName());
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    /**
     * Helper method to get a parameter value with type casting.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getParameter(Map<String, Object> parameters, String name, Class<T> type) {
        Object value = parameters.get(name);
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        throw new IllegalArgumentException(
            String.format("Parameter '%s' expected type %s but got %s", 
                name, type.getSimpleName(), value.getClass().getSimpleName()));
    }
    
    /**
     * Helper method to get a required parameter value with type casting.
     */
    protected <T> T getRequiredParameter(Map<String, Object> parameters, String name, Class<T> type) {
        T value = getParameter(parameters, name, type);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + name + "' is missing");
        }
        return value;
    }
}