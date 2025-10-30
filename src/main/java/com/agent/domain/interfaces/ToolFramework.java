package com.agent.domain.interfaces;

import com.agent.domain.model.Tool;
import com.agent.domain.model.ToolDefinition;
import com.agent.domain.model.ToolResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for the tool framework that manages tool registration, discovery, and execution.
 */
public interface ToolFramework {
    
    /**
     * Register a new tool with the framework.
     * 
     * @param tool The tool implementation to register
     */
    void registerTool(Tool tool);
    
    /**
     * Execute a tool synchronously with the given parameters.
     * 
     * @param toolName The name of the tool to execute
     * @param parameters The parameters to pass to the tool
     * @return The result of the tool execution
     */
    ToolResult executeTool(String toolName, Map<String, Object> parameters);
    
    /**
     * Get a list of all available tools and their definitions.
     * 
     * @return List of tool definitions
     */
    List<ToolDefinition> getAvailableTools();
    
    /**
     * Execute a tool asynchronously with the given parameters.
     * 
     * @param toolName The name of the tool to execute
     * @param parameters The parameters to pass to the tool
     * @return A CompletableFuture containing the tool execution result
     */
    CompletableFuture<ToolResult> executeToolAsync(String toolName, Map<String, Object> parameters);
}