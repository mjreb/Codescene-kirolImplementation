package com.agent.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Map;

/**
 * Configuration properties for the Agent application.
 */
@ConfigurationProperties(prefix = "agent")
public record AgentConfigurationProperties(
    LlmProperties llm,
    TokenProperties token,
    MemoryProperties memory,
    ToolsProperties tools,
    MonitoringProperties monitoring
) {
    
    public record LlmProperties(
        Map<String, ProviderProperties> providers
    ) {}
    
    public record ProviderProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        RetryProperties retry,
        RateLimitProperties rateLimit
    ) {}
    
    public record RetryProperties(
        int maxAttempts,
        long backoffMs,
        double backoffMultiplier
    ) {
        public RetryProperties() {
            this(3, 1000, 2.0);
        }
    }
    
    public record RateLimitProperties(
        int requestsPerMinute,
        int requestsPerHour
    ) {
        public RateLimitProperties() {
            this(60, 1000);
        }
    }
    
    public record TokenProperties(
        DefaultLimits defaultLimits
    ) {}
    
    public record DefaultLimits(
        int maxTokensPerRequest,
        int maxTokensPerConversation,
        int maxTokensPerDay
    ) {}
    
    public record MemoryProperties(
        boolean shortTermEnabled,
        boolean longTermEnabled,
        int shortTermTtlMinutes,
        int maxConversationHistory,
        boolean semanticSearchEnabled,
        double similarityThreshold
    ) {}
    
    public record ToolsProperties(
        boolean enabledByDefault,
        int timeoutSeconds,
        Map<String, ToolConfig> tools
    ) {}
    
    public record ToolConfig(
        boolean enabled,
        Map<String, Object> configuration,
        String[] requiredPermissions
    ) {}
    
    public record MonitoringProperties(
        MetricsProperties metrics,
        TracingProperties tracing,
        HealthProperties health
    ) {}
    
    public record MetricsProperties(
        boolean enabled,
        String prefix,
        Map<String, String> tags
    ) {
        public MetricsProperties() {
            this(true, "agent", Map.of());
        }
    }
    
    public record TracingProperties(
        boolean enabled,
        String serviceName,
        double samplingRate
    ) {
        public TracingProperties() {
            this(true, "agent-application", 0.1);
        }
    }
    
    public record HealthProperties(
        boolean detailedEnabled,
        int timeoutSeconds
    ) {
        public HealthProperties() {
            this(true, 10);
        }
    }
}