package com.agent.infrastructure.config;

import com.agent.infrastructure.llm.LLMProviderManagerImpl;
import com.agent.infrastructure.llm.LLMProviderConfiguration;
import com.agent.infrastructure.llm.providers.AnthropicProvider;
import com.agent.infrastructure.llm.providers.OllamaProvider;
import com.agent.infrastructure.llm.providers.OpenAIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for LLM providers.
 */
@Configuration
@EnableConfigurationProperties(LLMProviderConfiguration.class)
public class LLMProviderConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMProviderConfig.class);
    
    @Bean
    public LLMProviderManagerImpl llmProviderManager(LLMProviderConfiguration configuration) {
        LLMProviderManagerImpl manager = new LLMProviderManagerImpl();
        
        // Register providers based on configuration
        configuration.getProviders().forEach((providerId, settings) -> {
            if (settings.isEnabled()) {
                try {
                    com.agent.domain.model.LLMProviderConfig providerConfig = 
                        settings.toLLMProviderConfig(providerId);
                    
                    switch (providerId.toLowerCase()) {
                        case "openai":
                            manager.registerProvider(new OpenAIProvider(providerConfig));
                            break;
                        case "anthropic":
                            manager.registerProvider(new AnthropicProvider(providerConfig));
                            break;
                        case "ollama":
                            manager.registerProvider(new OllamaProvider(providerConfig));
                            break;
                        default:
                            logger.warn("Unknown provider type: {}", providerId);
                    }
                    
                    logger.info("Registered LLM provider: {}", providerId);
                } catch (Exception e) {
                    logger.error("Failed to register provider {}: {}", providerId, e.getMessage());
                }
            }
        });
        
        return manager;
    }
}