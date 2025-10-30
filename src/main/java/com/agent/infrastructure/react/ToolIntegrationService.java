package com.agent.infrastructure.react;

import com.agent.domain.interfaces.ToolFramework;
import com.agent.domain.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for integrating the ReAct Engine with the Tool Framework.
 * Handles tool selection, parameter extraction, and result processing.
 */
@Service
public class ToolIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolIntegrationService.class);
    
    private final ToolFramework toolFramework;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public ToolIntegrationService(ToolFramework toolFramework) {
        this.toolFramework = toolFramework;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get available tools formatted for LLM prompt inclusion.
     */
    public String getAvailableToolsDescription() {
        List<ToolDefinition> tools = toolFramework.getAvailableTools();
        
        if (tools.isEmpty()) {
            return "No tools are currently available.";
        }
        
        StringBuilder description = new StringBuilder();
        description.append("Available Tools:\n");
        
        for (ToolDefinition tool : tools) {
            description.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
            
            if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                description.append("  Parameters:\n");
                for (Map.Entry<String, ParameterDefinition> entry : tool.getParameters().entrySet()) {
                    ParameterDefinition param = entry.getValue();
                    String required = param.isRequired() ? " (required)" : " (optional)";
                    description.append(String.format("    - %s (%s): %s%s\n", 
                        entry.getKey(), param.getType(), param.getDescription(), required));
                }
            }
            description.append("\n");
        }
        
        return description.toString();
    }
    
    /**
     * Validate that a tool exists and can be executed.
     */
    public boolean isToolAvailable(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return false;
        }
        
        return toolFramework.getAvailableTools().stream()
                .anyMatch(tool -> tool.getName().equals(toolName.trim()));
    }
    
    /**
     * Get detailed information about a specific tool.
     */
    public Optional<ToolDefinition> getToolDefinition(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return toolFramework.getAvailableTools().stream()
                .filter(tool -> tool.getName().equals(toolName.trim()))
                .findFirst();
    }
    
    /**
     * Execute a tool and return the result with enhanced error handling.
     */
    public ToolResult executeToolSafely(String toolName, Map<String, Object> parameters) {
        logger.debug("Executing tool: {} with parameters: {}", toolName, parameters);
        
        try {
            // Validate tool exists
            if (!isToolAvailable(toolName)) {
                return createErrorResult(toolName, "TOOL_NOT_FOUND", 
                    "Tool '" + toolName + "' is not available. Available tools: " + 
                    getAvailableToolNames());
            }
            
            // Validate parameters
            Optional<ToolDefinition> toolDef = getToolDefinition(toolName);
            if (toolDef.isPresent()) {
                String validationError = validateParameters(toolDef.get(), parameters);
                if (validationError != null) {
                    return createErrorResult(toolName, "INVALID_PARAMETERS", validationError);
                }
            }
            
            // Execute the tool
            ToolResult result = toolFramework.executeTool(toolName, parameters);
            
            // Enhance the result with additional metadata
            enhanceToolResult(result, toolName, parameters);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing tool {}: {}", toolName, e.getMessage(), e);
            return createErrorResult(toolName, "EXECUTION_ERROR", 
                "Tool execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Suggest alternative tools when a tool is not available or fails.
     */
    public List<String> suggestAlternativeTools(String failedToolName, String context) {
        List<ToolDefinition> availableTools = toolFramework.getAvailableTools();
        
        // Simple suggestion based on tool categories and names
        return availableTools.stream()
                .filter(tool -> !tool.getName().equals(failedToolName))
                .filter(tool -> isToolRelevant(tool, failedToolName, context))
                .map(ToolDefinition::getName)
                .limit(3)
                .collect(Collectors.toList());
    }
    
    /**
     * Parse tool parameters from various formats (JSON, key-value pairs, etc.).
     */
    public Map<String, Object> parseToolParameters(String parametersStr) {
        if (parametersStr == null || parametersStr.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        parametersStr = parametersStr.trim();
        
        try {
            // Try JSON format first
            if (parametersStr.startsWith("{") && parametersStr.endsWith("}")) {
                return objectMapper.readValue(parametersStr, new TypeReference<Map<String, Object>>() {});
            }
            
            // Try simple key-value format
            return parseKeyValueParameters(parametersStr);
            
        } catch (Exception e) {
            logger.warn("Failed to parse tool parameters: {}", parametersStr, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Format tool result for inclusion in LLM observation.
     */
    public String formatToolResultForObservation(ToolResult result) {
        if (result == null) {
            return "No result available";
        }
        
        StringBuilder observation = new StringBuilder();
        
        if (result.isSuccess()) {
            observation.append("Tool '").append(result.getToolName()).append("' executed successfully.\n");
            observation.append("Result: ").append(formatResultValue(result.getResult()));
            
            if (result.getDurationMs() > 0) {
                observation.append("\nExecution time: ").append(result.getDurationMs()).append("ms");
            }
        } else {
            observation.append("Tool '").append(result.getToolName()).append("' failed.\n");
            observation.append("Error: ").append(result.getErrorMessage());
            
            // Suggest alternatives if available
            List<String> alternatives = suggestAlternativeTools(result.getToolName(), result.getErrorMessage());
            if (!alternatives.isEmpty()) {
                observation.append("\nAlternative tools you could try: ")
                          .append(String.join(", ", alternatives));
            }
        }
        
        return observation.toString();
    }
    
    /**
     * Get a list of available tool names.
     */
    private String getAvailableToolNames() {
        return toolFramework.getAvailableTools().stream()
                .map(ToolDefinition::getName)
                .collect(Collectors.joining(", "));
    }
    
    /**
     * Validate parameters against tool definition.
     */
    private String validateParameters(ToolDefinition toolDef, Map<String, Object> parameters) {
        if (toolDef.getParameters() == null) {
            return null; // No parameters required
        }
        
        // Check required parameters
        for (Map.Entry<String, ParameterDefinition> entry : toolDef.getParameters().entrySet()) {
            String paramName = entry.getKey();
            ParameterDefinition paramDef = entry.getValue();
            
            if (paramDef.isRequired() && (parameters == null || !parameters.containsKey(paramName))) {
                return "Missing required parameter: " + paramName;
            }
        }
        
        return null; // Parameters are valid
    }
    
    /**
     * Enhance tool result with additional metadata.
     */
    private void enhanceToolResult(ToolResult result, String toolName, Map<String, Object> parameters) {
        if (result.getMetadata() == null) {
            result.setMetadata(new HashMap<>());
        }
        
        result.getMetadata().put("executed_by", "react_engine");
        result.getMetadata().put("parameter_count", parameters != null ? parameters.size() : 0);
        
        // Add execution context
        if (result.isSuccess()) {
            result.getMetadata().put("execution_status", "success");
        } else {
            result.getMetadata().put("execution_status", "failed");
        }
    }
    
    /**
     * Check if a tool is relevant as an alternative.
     */
    private boolean isToolRelevant(ToolDefinition tool, String failedToolName, String context) {
        // Simple relevance check based on category and name similarity
        if (tool.getCategory() != null && failedToolName.contains(tool.getCategory())) {
            return true;
        }
        
        // Check for similar names
        String toolName = tool.getName().toLowerCase();
        String failed = failedToolName.toLowerCase();
        
        return toolName.contains(failed.substring(0, Math.min(3, failed.length()))) ||
               failed.contains(toolName.substring(0, Math.min(3, toolName.length())));
    }
    
    /**
     * Parse key-value parameter format.
     */
    private Map<String, Object> parseKeyValueParameters(String parametersStr) {
        Map<String, Object> parameters = new HashMap<>();
        
        // Handle simple comma-separated key:value pairs
        String[] pairs = parametersStr.split(",");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("[\"\']", "");
                String value = keyValue[1].trim().replaceAll("[\"\']", "");
                
                // Try to convert to appropriate type
                Object convertedValue = convertValue(value);
                parameters.put(key, convertedValue);
            }
        }
        
        return parameters;
    }
    
    /**
     * Convert string value to appropriate type.
     */
    private Object convertValue(String value) {
        if (value == null) {
            return null;
        }
        
        value = value.trim();
        
        // Try boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        
        // Try integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        
        // Try double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}
        
        // Return as string
        return value;
    }
    
    /**
     * Format result value for display.
     */
    private String formatResultValue(Object result) {
        if (result == null) {
            return "null";
        }
        
        if (result instanceof String) {
            return (String) result;
        }
        
        if (result instanceof Number) {
            return result.toString();
        }
        
        if (result instanceof Boolean) {
            return result.toString();
        }
        
        // For complex objects, try to serialize to JSON
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return result.toString();
        }
    }
    
    /**
     * Create an error result.
     */
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
}