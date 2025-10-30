package com.agent.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents the definition and metadata of a tool.
 */
public class ToolDefinition {
    private String name;
    private String description;
    private Map<String, ParameterDefinition> parameters;
    private boolean isAsync;
    private List<String> requiredPermissions;
    private String category;
    
    public ToolDefinition() {}
    
    public ToolDefinition(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Map<String, ParameterDefinition> getParameters() { return parameters; }
    public void setParameters(Map<String, ParameterDefinition> parameters) { this.parameters = parameters; }
    
    public boolean isAsync() { return isAsync; }
    public void setAsync(boolean async) { isAsync = async; }
    
    public List<String> getRequiredPermissions() { return requiredPermissions; }
    public void setRequiredPermissions(List<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}