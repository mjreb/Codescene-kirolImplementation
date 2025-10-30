package com.agent.infrastructure.security.service;

import com.agent.domain.interfaces.ToolFramework;
import com.agent.domain.model.Tool;
import com.agent.domain.model.ToolDefinition;
import com.agent.domain.model.ToolResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Secure wrapper for ToolFramework that enforces authorization
 */
@Component
public class SecureToolFramework implements ToolFramework {

    private final ToolFramework delegate;
    private final AuthorizationService authorizationService;

    public SecureToolFramework(ToolFramework delegate, AuthorizationService authorizationService) {
        this.delegate = delegate;
        this.authorizationService = authorizationService;
    }

    @Override
    public void registerTool(Tool tool) {
        // Only admins can register tools
        if (!authorizationService.hasRole("ADMIN")) {
            throw new AccessDeniedException("Only administrators can register tools");
        }
        delegate.registerTool(tool);
    }

    @Override
    public ToolResult executeTool(String toolName, Map<String, Object> parameters) {
        // Check if user can execute this tool
        if (!authorizationService.canExecuteTool(toolName)) {
            throw new AccessDeniedException("Access denied: cannot execute tool " + toolName);
        }
        
        return delegate.executeTool(toolName, parameters);
    }

    @Override
    public List<ToolDefinition> getAvailableTools() {
        List<ToolDefinition> allTools = delegate.getAvailableTools();
        
        // Filter tools based on user permissions
        return allTools.stream()
                .filter(tool -> authorizationService.canExecuteTool(tool.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<ToolResult> executeToolAsync(String toolName, Map<String, Object> parameters) {
        // Check if user can execute this tool
        if (!authorizationService.canExecuteTool(toolName)) {
            return CompletableFuture.failedFuture(
                new AccessDeniedException("Access denied: cannot execute tool " + toolName)
            );
        }
        
        return delegate.executeToolAsync(toolName, parameters);
    }

    /**
     * Check if tool is registered and user can access it
     */
    public boolean isToolRegistered(String toolName) {
        // Check if tool exists in delegate and user can execute it
        List<ToolDefinition> availableTools = getAvailableTools();
        return availableTools.stream()
                .anyMatch(tool -> tool.getName().equals(toolName));
    }

    /**
     * Get tool definition if user has permission
     */
    public ToolDefinition getToolDefinition(String toolName) {
        if (!authorizationService.canExecuteTool(toolName)) {
            throw new AccessDeniedException("Access denied: cannot access tool definition for " + toolName);
        }
        
        List<ToolDefinition> availableTools = delegate.getAvailableTools();
        return availableTools.stream()
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }
}