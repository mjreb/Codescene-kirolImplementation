package com.agent.infrastructure.memory;

import com.agent.domain.model.MemoryConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for memory management components.
 */
@Configuration
public class MemoryConfig {
    
    @Bean
    @ConfigurationProperties(prefix = "agent.memory")
    public MemoryConfiguration memoryConfiguration() {
        return new MemoryConfiguration();
    }
}