package com.agent.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for monitoring and observability features.
 */
@Configuration
@EnableConfigurationProperties(AgentConfigurationProperties.class)
public class MonitoringConfiguration {
    
    private final AgentConfigurationProperties properties;
    
    public MonitoringConfiguration(AgentConfigurationProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            // Add common tags to all metrics
            registry.config()
                    .commonTags("application", "agent-application")
                    .commonTags("version", getClass().getPackage().getImplementationVersion() != null ? 
                               getClass().getPackage().getImplementationVersion() : "unknown");
            
            // Add custom tags from configuration
            if (properties.monitoring() != null && 
                properties.monitoring().metrics() != null && 
                properties.monitoring().metrics().tags() != null) {
                
                properties.monitoring().metrics().tags().forEach(registry.config()::commonTags);
            }
            
            // Add meter filters
            registry.config()
                    .meterFilter(MeterFilter.deny(id -> {
                        String name = id.getName();
                        // Filter out noisy metrics
                        return name.startsWith("jvm.gc.pause") ||
                               name.startsWith("hikaricp.connections.idle") ||
                               name.startsWith("tomcat.sessions");
                    }))
                    .meterFilter(MeterFilter.accept());
        };
    }
    
    @Bean
    public MeterFilter customMeterFilter() {
        return MeterFilter.renameTag("agent.llm.requests", "provider", "llm_provider");
    }
}