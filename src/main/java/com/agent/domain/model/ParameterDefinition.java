package com.agent.domain.model;

/**
 * Represents the definition of a tool parameter.
 */
public class ParameterDefinition {
    private String name;
    private String type;
    private String description;
    private boolean required;
    private Object defaultValue;
    private String pattern;
    private Object minValue;
    private Object maxValue;
    
    public ParameterDefinition() {}
    
    public ParameterDefinition(String name, String type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    
    public Object getMinValue() { return minValue; }
    public void setMinValue(Object minValue) { this.minValue = minValue; }
    
    public Object getMaxValue() { return maxValue; }
    public void setMaxValue(Object maxValue) { this.maxValue = maxValue; }
}