package com.agent.infrastructure.llm;

import com.agent.domain.model.LLMRequest;
import com.agent.domain.model.LLMResponse;
import com.agent.domain.model.ProviderHealth;

/**
 * Base interface for LLM provider implementations.
 */
public interface LLMProvider {
    
    /**
     * Generate a response using this LLM provider.
     * 
     * @param request The LLM request
     * @return The response from the provider
     */
    LLMResponse generateResponse(LLMRequest request);
    
    /**
     * Check the health of this provider.
     * 
     * @return The health status
     */
    ProviderHealth checkHealth();
    
    /**
     * Get the provider identifier.
     * 
     * @return The provider ID
     */
    String getProviderId();
    
    /**
     * Check if this provider supports the given model.
     * 
     * @param model The model name
     * @return true if supported, false otherwise
     */
    boolean supportsModel(String model);
    
    /**
     * Estimate token count for the given request.
     * 
     * @param request The LLM request
     * @return Estimated token count
     */
    int estimateTokenCount(LLMRequest request);
}