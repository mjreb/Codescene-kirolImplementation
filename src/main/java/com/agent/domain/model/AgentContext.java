package com.agent.domain.model;

import java.util.Map;

/**
 * Represents the context information for an agent during conversation processing.
 */
public class AgentContext {
    private String agentId;
    private String userId;
    private Map<String, Object> sessionData;
    private AgentConfiguration configuration;
    
    public AgentContext() {}
    
    public AgentContext(String agentId, String userId, Map<String, Object> sessionData, AgentConfiguration configuration) {
        this.agentId = agentId;
        this.userId = userId;
        this.sessionData = sessionData;
        this.configuration = configuration;
    }
    
    // Getters and setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Map<String, Object> getSessionData() { return sessionData; }
    public void setSessionData(Map<String, Object> sessionData) { this.sessionData = sessionData; }
    
    public AgentConfiguration getConfiguration() { return configuration; }
    public void setConfiguration(AgentConfiguration configuration) { this.configuration = configuration; }
}