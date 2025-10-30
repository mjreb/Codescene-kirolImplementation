package com.agent.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration properties for the agent application.
 */
@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentConfiguration {
    
    private LlmConfig llm = new LlmConfig();
    private TokenConfig token = new TokenConfig();
    private MemoryConfig memory = new MemoryConfig();
    private ToolsConfig tools = new ToolsConfig();
    
    // Getters and setters
    public LlmConfig getLlm() { return llm; }
    public void setLlm(LlmConfig llm) { this.llm = llm; }
    
    public TokenConfig getToken() { return token; }
    public void setToken(TokenConfig token) { this.token = token; }
    
    public MemoryConfig getMemory() { return memory; }
    public void setMemory(MemoryConfig memory) { this.memory = memory; }
    
    public ToolsConfig getTools() { return tools; }
    public void setTools(ToolsConfig tools) { this.tools = tools; }
    
    public static class LlmConfig {
        private Map<String, ProviderConfig> providers;
        
        public Map<String, ProviderConfig> getProviders() { return providers; }
        public void setProviders(Map<String, ProviderConfig> providers) { this.providers = providers; }
    }
    
    public static class ProviderConfig {
        private boolean enabled;
        private String apiKey;
        private String baseUrl;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
    
    public static class TokenConfig {
        private DefaultLimits defaultLimits = new DefaultLimits();
        
        public DefaultLimits getDefaultLimits() { return defaultLimits; }
        public void setDefaultLimits(DefaultLimits defaultLimits) { this.defaultLimits = defaultLimits; }
        
        public static class DefaultLimits {
            private int maxTokensPerRequest;
            private int maxTokensPerConversation;
            private int maxTokensPerDay;
            
            public int getMaxTokensPerRequest() { return maxTokensPerRequest; }
            public void setMaxTokensPerRequest(int maxTokensPerRequest) { this.maxTokensPerRequest = maxTokensPerRequest; }
            
            public int getMaxTokensPerConversation() { return maxTokensPerConversation; }
            public void setMaxTokensPerConversation(int maxTokensPerConversation) { this.maxTokensPerConversation = maxTokensPerConversation; }
            
            public int getMaxTokensPerDay() { return maxTokensPerDay; }
            public void setMaxTokensPerDay(int maxTokensPerDay) { this.maxTokensPerDay = maxTokensPerDay; }
        }
    }
    
    public static class MemoryConfig {
        private ShortTermConfig shortTerm = new ShortTermConfig();
        private LongTermConfig longTerm = new LongTermConfig();
        
        public ShortTermConfig getShortTerm() { return shortTerm; }
        public void setShortTerm(ShortTermConfig shortTerm) { this.shortTerm = shortTerm; }
        
        public LongTermConfig getLongTerm() { return longTerm; }
        public void setLongTerm(LongTermConfig longTerm) { this.longTerm = longTerm; }
        
        public static class ShortTermConfig {
            private boolean enabled;
            private int ttlMinutes;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public int getTtlMinutes() { return ttlMinutes; }
            public void setTtlMinutes(int ttlMinutes) { this.ttlMinutes = ttlMinutes; }
        }
        
        public static class LongTermConfig {
            private boolean enabled;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }
    }
    
    public static class ToolsConfig {
        private boolean enabledByDefault;
        private int timeoutSeconds;
        
        public boolean isEnabledByDefault() { return enabledByDefault; }
        public void setEnabledByDefault(boolean enabledByDefault) { this.enabledByDefault = enabledByDefault; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}