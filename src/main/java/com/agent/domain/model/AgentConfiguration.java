package com.agent.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents the configuration settings for an agent.
 */
public class AgentConfiguration {
    private String defaultLLMProvider;
    private String defaultModel;
    private Map<String, Object> llmParameters;
    private List<String> enabledTools;
    private TokenLimits tokenLimits;
    private MemoryConfiguration memoryConfig;
    private Map<String, Object> customSettings;
    
    public AgentConfiguration() {}
    
    // Getters and setters
    public String getDefaultLLMProvider() { return defaultLLMProvider; }
    public void setDefaultLLMProvider(String defaultLLMProvider) { this.defaultLLMProvider = defaultLLMProvider; }
    
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    
    public Map<String, Object> getLlmParameters() { return llmParameters; }
    public void setLlmParameters(Map<String, Object> llmParameters) { this.llmParameters = llmParameters; }
    
    public List<String> getEnabledTools() { return enabledTools; }
    public void setEnabledTools(List<String> enabledTools) { this.enabledTools = enabledTools; }
    
    public TokenLimits getTokenLimits() { return tokenLimits; }
    public void setTokenLimits(TokenLimits tokenLimits) { this.tokenLimits = tokenLimits; }
    
    public MemoryConfiguration getMemoryConfig() { return memoryConfig; }
    public void setMemoryConfig(MemoryConfiguration memoryConfig) { this.memoryConfig = memoryConfig; }
    
    public Map<String, Object> getCustomSettings() { return customSettings; }
    public void setCustomSettings(Map<String, Object> customSettings) { this.customSettings = customSettings; }
}