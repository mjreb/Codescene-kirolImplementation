package com.agent.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Configuration settings for tools including permissions and enablement flags.
 */
public class ToolConfig {
    private String toolName;
    private String implementationClass;
    private Map<String, Object> configuration;
    private List<String> requiredPermissions;
    private boolean enabled;
    private int timeoutSeconds;
    private boolean asyncSupported;
    private String description;
    private String version;
    
    public ToolConfig() {
        this.enabled = true;
        this.timeoutSeconds = 30;
        this.asyncSupported = false;
    }
    
    public ToolConfig(String toolName, String implementationClass) {
        this();
        this.toolName = toolName;
        this.implementationClass = implementationClass;
    }
    
    /**
     * Checks if the tool has a specific permission.
     */
    public boolean hasPermission(String permission) {
        return requiredPermissions != null && requiredPermissions.contains(permission);
    }
    
    /**
     * Adds a required permission to the tool.
     */
    public void addRequiredPermission(String permission) {
        if (requiredPermissions != null && !requiredPermissions.contains(permission)) {
            requiredPermissions.add(permission);
        }
    }
    
    /**
     * Gets a configuration value by key.
     */
    public Object getConfigurationValue(String key) {
        return configuration != null ? configuration.get(key) : null;
    }
    
    /**
     * Sets a configuration value.
     */
    public void setConfigurationValue(String key, Object value) {
        if (configuration != null) {
            configuration.put(key, value);
        }
    }
    
    // Getters and setters
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    
    public String getImplementationClass() { return implementationClass; }
    public void setImplementationClass(String implementationClass) { this.implementationClass = implementationClass; }
    
    public Map<String, Object> getConfiguration() { return configuration; }
    public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
    
    public List<String> getRequiredPermissions() { return requiredPermissions; }
    public void setRequiredPermissions(List<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public boolean isAsyncSupported() { return asyncSupported; }
    public void setAsyncSupported(boolean asyncSupported) { this.asyncSupported = asyncSupported; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}