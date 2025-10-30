package com.agent.domain.interfaces;

import com.agent.domain.model.LLMRequest;
import com.agent.domain.model.LLMResponse;
import com.agent.domain.model.ProviderHealth;

import java.util.List;

/**
 * Interface for managing Language Model providers.
 * Abstracts LLM provider interactions and manages provider selection.
 */
public interface LLMProviderManager {
    
    /**
     * Generate a response using the specified LLM provider.
     * 
     * @param request The LLM request containing prompt and parameters
     * @param providerId The identifier of the LLM provider to use
     * @return The response from the LLM provider
     */
    LLMResponse generateResponse(LLMRequest request, String providerId);
    
    /**
     * Get a list of all available LLM providers.
     * 
     * @return List of provider identifiers
     */
    List<String> getAvailableProviders();
    
    /**
     * Check the health status of a specific LLM provider.
     * 
     * @param providerId The identifier of the provider to check
     * @return The health status of the provider
     */
    ProviderHealth checkProviderHealth(String providerId);
}