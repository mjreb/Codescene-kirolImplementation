package com.agent.infrastructure.config;

import com.agent.domain.interfaces.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ConfigurationManager that provides environment-specific
 * configuration management with runtime update capabilities.
 */
@Component
@RefreshScope
public class ConfigurationManagerImpl implements ConfigurationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManagerImpl.class);
    private static final String RUNTIME_CONFIG_SOURCE = "runtimeConfiguration";
    
    private final Environment environment;
    private final ConfigurableEnvironment configurableEnvironment;
    private final ContextRefresher contextRefresher;
    private final Map<String, String> runtimeProperties;
    
    @Autowired
    public ConfigurationManagerImpl(Environment environment, 
                                  ConfigurableEnvironment configurableEnvironment,
                                  ContextRefresher contextRefresher) {
        this.environment = environment;
        this.configurableEnvironment = configurableEnvironment;
        this.contextRefresher = contextRefresher;
        this.runtimeProperties = new ConcurrentHashMap<>();
        
        // Add runtime property source
        addRuntimePropertySource();
    }
    
    @Override
    public Optional<String> getConfigValue(String key) {
        // First check runtime properties
        if (runtimeProperties.containsKey(key)) {
            return Optional.of(runtimeProperties.get(key));
        }
        
        // Then check environment
        String value = environment.getProperty(key);
        return Optional.ofNullable(value);
    }
    
    @Override
    public String getConfigValue(String key, String defaultValue) {
        return getConfigValue(key).orElse(defaultValue);
    }
    
    @Override
    public Map<String, String> getConfigProperties(String prefix) {
        Map<String, String> properties = new HashMap<>();
        
        // Get properties from environment
        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
        propertySources.forEach(propertySource -> {
            if (propertySource instanceof MapPropertySource mapSource) {
                mapSource.getSource().forEach((key, value) -> {
                    String keyStr = key.toString();
                    if (keyStr.startsWith(prefix)) {
                        properties.put(keyStr, value != null ? value.toString() : null);
                    }
                });
            }
        });
        
        // Override with runtime properties
        runtimeProperties.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                properties.put(key, value);
            }
        });
        
        return properties;
    }
    
    @Override
    public boolean updateConfigValue(String key, String value) {
        try {
            logger.info("Updating configuration key: {} with value: {}", key, 
                       value != null && key.toLowerCase().contains("secret") ? "***" : value);
            
            runtimeProperties.put(key, value);
            
            // Update the runtime property source
            updateRuntimePropertySource();
            
            logger.info("Successfully updated configuration key: {}", key);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update configuration key: {}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean refreshConfiguration() {
        try {
            logger.info("Refreshing configuration from external sources");
            contextRefresher.refresh();
            logger.info("Configuration refresh completed successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to refresh configuration", e);
            return false;
        }
    }
    
    @Override
    public boolean validateConfiguration() {
        try {
            // Define required configuration keys
            String[] requiredKeys = {
                "spring.application.name",
                "server.port",
                "app.security.jwt.secret"
            };
            
            for (String key : requiredKeys) {
                if (!hasConfigKey(key)) {
                    logger.error("Required configuration key missing: {}", key);
                    return false;
                }
            }
            
            // Validate JWT secret length
            String jwtSecret = getConfigValue("app.security.jwt.secret", "");
            if (jwtSecret.length() < 32) {
                logger.error("JWT secret must be at least 32 characters long");
                return false;
            }
            
            logger.info("Configuration validation passed");
            return true;
        } catch (Exception e) {
            logger.error("Configuration validation failed", e);
            return false;
        }
    }
    
    @Override
    public String getEnvironmentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0 ? activeProfiles[0] : "default";
    }
    
    @Override
    public boolean hasConfigKey(String key) {
        return runtimeProperties.containsKey(key) || environment.containsProperty(key);
    }
    
    private void addRuntimePropertySource() {
        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
        if (!propertySources.contains(RUNTIME_CONFIG_SOURCE)) {
            propertySources.addFirst(new MapPropertySource(RUNTIME_CONFIG_SOURCE, new HashMap<>(runtimeProperties)));
        }
    }
    
    private void updateRuntimePropertySource() {
        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
        propertySources.replace(RUNTIME_CONFIG_SOURCE, 
                               new MapPropertySource(RUNTIME_CONFIG_SOURCE, new HashMap<>(runtimeProperties)));
    }
}