package com.agent.domain.model;

import java.util.Map;

/**
 * Interface for tool implementations that can be executed by agents.
 */
public interface Tool {
    
    /**
     * Get the definition of this tool.
     * 
     * @return The tool definition
     */
    ToolDefinition getDefinition();
    
    /**
     * Execute the tool with the given parameters.
     * 
     * @param parameters The parameters for tool execution
     * @return The result of the tool execution
     */
    ToolResult execute(Map<String, Object> parameters);
    
    /**
     * Validate the parameters before execution.
     * 
     * @param parameters The parameters to validate
     * @return true if parameters are valid, false otherwise
     */
    boolean validateParameters(Map<String, Object> parameters);
}