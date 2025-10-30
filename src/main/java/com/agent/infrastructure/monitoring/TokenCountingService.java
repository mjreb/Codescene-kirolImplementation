package com.agent.infrastructure.monitoring;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for counting tokens and calculating costs across different LLM providers.
 */
@Service
public class TokenCountingService {
    
    // Cost per 1K tokens for different providers and models (in USD)
    private static final Map<String, Map<String, Double>> INPUT_COSTS = new HashMap<>();
    private static final Map<String, Map<String, Double>> OUTPUT_COSTS = new HashMap<>();
    
    static {
        // OpenAI pricing (as of 2024)
        Map<String, Double> openaiInputCosts = new HashMap<>();
        openaiInputCosts.put("gpt-3.5-turbo", 0.0015);
        openaiInputCosts.put("gpt-4", 0.03);
        openaiInputCosts.put("gpt-4-turbo", 0.01);
        INPUT_COSTS.put("openai", openaiInputCosts);
        
        Map<String, Double> openaiOutputCosts = new HashMap<>();
        openaiOutputCosts.put("gpt-3.5-turbo", 0.002);
        openaiOutputCosts.put("gpt-4", 0.06);
        openaiOutputCosts.put("gpt-4-turbo", 0.03);
        OUTPUT_COSTS.put("openai", openaiOutputCosts);
        
        // Anthropic pricing
        Map<String, Double> anthropicInputCosts = new HashMap<>();
        anthropicInputCosts.put("claude-3-haiku", 0.00025);
        anthropicInputCosts.put("claude-3-sonnet", 0.003);
        anthropicInputCosts.put("claude-3-opus", 0.015);
        INPUT_COSTS.put("anthropic", anthropicInputCosts);
        
        Map<String, Double> anthropicOutputCosts = new HashMap<>();
        anthropicOutputCosts.put("claude-3-haiku", 0.00125);
        anthropicOutputCosts.put("claude-3-sonnet", 0.015);
        anthropicOutputCosts.put("claude-3-opus", 0.075);
        OUTPUT_COSTS.put("anthropic", anthropicOutputCosts);
        
        // Ollama (local models - no cost)
        Map<String, Double> ollamaCosts = new HashMap<>();
        ollamaCosts.put("llama2", 0.0);
        ollamaCosts.put("mistral", 0.0);
        ollamaCosts.put("codellama", 0.0);
        INPUT_COSTS.put("ollama", ollamaCosts);
        OUTPUT_COSTS.put("ollama", ollamaCosts);
    }
    
    /**
     * Estimate token count for a given text.
     * This is a simplified estimation - in production, you'd use provider-specific tokenizers.
     */
    public int estimateTokens(String text, String providerId, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Simple estimation: ~4 characters per token for most models
        // This is a rough approximation - real implementations should use proper tokenizers
        int estimatedTokens = (int) Math.ceil(text.length() / 4.0);
        
        // Apply provider-specific adjustments
        switch (providerId.toLowerCase()) {
            case "openai":
                // OpenAI models tend to be more efficient
                return (int) (estimatedTokens * 0.9);
            case "anthropic":
                // Anthropic models have similar tokenization
                return estimatedTokens;
            case "ollama":
                // Local models may vary, use conservative estimate
                return (int) (estimatedTokens * 1.1);
            default:
                return estimatedTokens;
        }
    }
    
    /**
     * Calculate the estimated cost for token usage.
     */
    public double calculateCost(String providerId, String model, int inputTokens, int outputTokens) {
        if (providerId == null || model == null) {
            return 0.0;
        }
        
        Map<String, Double> providerInputCosts = INPUT_COSTS.get(providerId.toLowerCase());
        Map<String, Double> providerOutputCosts = OUTPUT_COSTS.get(providerId.toLowerCase());
        
        if (providerInputCosts == null || providerOutputCosts == null) {
            return 0.0; // Unknown provider
        }
        
        Double inputCostPer1K = providerInputCosts.get(model.toLowerCase());
        Double outputCostPer1K = providerOutputCosts.get(model.toLowerCase());
        
        if (inputCostPer1K == null || outputCostPer1K == null) {
            return 0.0; // Unknown model
        }
        
        double inputCost = (inputTokens / 1000.0) * inputCostPer1K;
        double outputCost = (outputTokens / 1000.0) * outputCostPer1K;
        
        return inputCost + outputCost;
    }
    
    /**
     * Get the cost per 1K input tokens for a provider and model.
     */
    public double getInputCostPer1K(String providerId, String model) {
        Map<String, Double> providerCosts = INPUT_COSTS.get(providerId.toLowerCase());
        if (providerCosts == null) {
            return 0.0;
        }
        return providerCosts.getOrDefault(model.toLowerCase(), 0.0);
    }
    
    /**
     * Get the cost per 1K output tokens for a provider and model.
     */
    public double getOutputCostPer1K(String providerId, String model) {
        Map<String, Double> providerCosts = OUTPUT_COSTS.get(providerId.toLowerCase());
        if (providerCosts == null) {
            return 0.0;
        }
        return providerCosts.getOrDefault(model.toLowerCase(), 0.0);
    }
    
    /**
     * Check if a provider and model combination is supported for cost calculation.
     */
    public boolean isCostCalculationSupported(String providerId, String model) {
        Map<String, Double> providerCosts = INPUT_COSTS.get(providerId.toLowerCase());
        return providerCosts != null && providerCosts.containsKey(model.toLowerCase());
    }
}