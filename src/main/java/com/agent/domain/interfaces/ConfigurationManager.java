package com.agent.domain.interfaces;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing application configuration with support for
 * environment-specific settings and runtime updates.
 */
public interface ConfigurationManager {
    
    /**
     * Get a configuration value by key
     * @param key the configuration key
     * @return the configuration value if present
     */
    Optional<String> getConfigValue(String key);
    
    /**
     * Get a configuration value with a default fallback
     * @param key the configuration key
     * @param defaultValue the default value if key is not found
     * @return the configuration value or default
     */
    String getConfigValue(String key, String defaultValue);
    
    /**
     * Get all configuration properties with a given prefix
     * @param prefix the configuration prefix
     * @return map of configuration properties
     */
    Map<String, String> getConfigProperties(String prefix);
    
    /**
     * Update a configuration value at runtime
     * @param key the configuration key
     * @param value the new configuration value
     * @return true if update was successful
     */
    boolean updateConfigValue(String key, String value);
    
    /**
     * Refresh configuration from external sources
     * @return true if refresh was successful
     */
    boolean refreshConfiguration();
    
    /**
     * Validate configuration for required properties
     * @return true if all required properties are present and valid
     */
    boolean validateConfiguration();
    
    /**
     * Get the current environment profile
     * @return the active environment profile
     */
    String getEnvironmentProfile();
    
    /**
     * Check if a configuration key exists
     * @param key the configuration key
     * @return true if the key exists
     */
    boolean hasConfigKey(String key);
}