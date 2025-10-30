package com.agent.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a request to an LLM provider.
 */
public class LLMRequest {
    private String prompt;
    private List<Message> messages;
    private Map<String, Object> parameters;
    private String model;
    private int maxTokens;
    private double temperature;
    
    public LLMRequest() {}
    
    public LLMRequest(String prompt, String model) {
        this.prompt = prompt;
        this.model = model;
    }
    
    // Getters and setters
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}