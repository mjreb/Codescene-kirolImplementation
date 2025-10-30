package com.agent.domain.exception;

/**
 * Exception for configuration and setup related errors.
 */
public class ConfigurationException extends AgentException {
    
    private final String configurationKey;
    private final String configurationSource;
    
    public ConfigurationException(String configurationKey, String message) {
        super("CONFIGURATION_ERROR", ErrorCategory.CONFIGURATION, message, false);
        this.configurationKey = configurationKey;
        this.configurationSource = null;
    }
    
    public ConfigurationException(String configurationKey, String configurationSource, String message) {
        super("CONFIGURATION_ERROR", ErrorCategory.CONFIGURATION, message, false);
        this.configurationKey = configurationKey;
        this.configurationSource = configurationSource;
    }
    
    public ConfigurationException(String configurationKey, String message, Throwable cause) {
        super("CONFIGURATION_ERROR", ErrorCategory.CONFIGURATION, message, cause, false);
        this.configurationKey = configurationKey;
        this.configurationSource = null;
    }
    
    public String getConfigurationKey() {
        return configurationKey;
    }
    
    public String getConfigurationSource() {
        return configurationSource;
    }
    
    @Override
    public String getUserMessage() {
        return "System configuration error. Please contact support.";
    }
    
    /**
     * Missing configuration exception.
     */
    public static class MissingConfigurationException extends ConfigurationException {
        public MissingConfigurationException(String configurationKey) {
            super(configurationKey, "Required configuration missing: " + configurationKey);
        }
        
        public MissingConfigurationException(String configurationKey, String configurationSource) {
            super(configurationKey, configurationSource, 
                    String.format("Required configuration missing: %s in %s", configurationKey, configurationSource));
        }
    }
    
    /**
     * Invalid configuration value exception.
     */
    public static class InvalidConfigurationException extends ConfigurationException {
        private final String actualValue;
        private final String expectedFormat;
        
        public InvalidConfigurationException(String configurationKey, String actualValue, String expectedFormat) {
            super(configurationKey, 
                    String.format("Invalid configuration value for '%s': got '%s', expected %s", 
                            configurationKey, actualValue, expectedFormat));
            this.actualValue = actualValue;
            this.expectedFormat = expectedFormat;
        }
        
        public String getActualValue() {
            return actualValue;
        }
        
        public String getExpectedFormat() {
            return expectedFormat;
        }
    }
    
    /**
     * Configuration validation exception.
     */
    public static class ValidationException extends ConfigurationException {
        public ValidationException(String configurationKey, String validationError) {
            super(configurationKey, "Configuration validation failed: " + validationError);
        }
        
        public ValidationException(String configurationKey, String validationError, Throwable cause) {
            super(configurationKey, "Configuration validation failed: " + validationError, cause);
        }
    }
}