package com.agent.infrastructure.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenCountingServiceTest {
    
    private TokenCountingService tokenCountingService;
    
    @BeforeEach
    void setUp() {
        tokenCountingService = new TokenCountingService();
    }
    
    @Test
    void estimateTokens_WithNullText_ShouldReturnZero() {
        // When
        int result = tokenCountingService.estimateTokens(null, "openai", "gpt-3.5-turbo");
        
        // Then
        assertEquals(0, result);
    }
    
    @Test
    void estimateTokens_WithEmptyText_ShouldReturnZero() {
        // When
        int result = tokenCountingService.estimateTokens("", "openai", "gpt-3.5-turbo");
        
        // Then
        assertEquals(0, result);
    }
    
    @Test
    void estimateTokens_WithOpenAIProvider_ShouldApplyCorrectMultiplier() {
        // Given
        String text = "Hello world"; // 11 characters, ~3 tokens with 4 chars per token
        
        // When
        int result = tokenCountingService.estimateTokens(text, "openai", "gpt-3.5-turbo");
        
        // Then
        // Expected: ceil(11/4) * 0.9 = 3 * 0.9 = 2.7 -> 2
        assertEquals(2, result);
    }
    
    @Test
    void estimateTokens_WithAnthropicProvider_ShouldReturnBasicEstimate() {
        // Given
        String text = "Hello world"; // 11 characters, ~3 tokens with 4 chars per token
        
        // When
        int result = tokenCountingService.estimateTokens(text, "anthropic", "claude-3-sonnet");
        
        // Then
        // Expected: ceil(11/4) = 3
        assertEquals(3, result);
    }
    
    @Test
    void estimateTokens_WithOllamaProvider_ShouldApplyConservativeMultiplier() {
        // Given
        String text = "Hello world"; // 11 characters, ~3 tokens with 4 chars per token
        
        // When
        int result = tokenCountingService.estimateTokens(text, "ollama", "llama2");
        
        // Then
        // Expected: ceil(11/4) * 1.1 = 3 * 1.1 = 3.3 -> 3
        assertEquals(3, result);
    }
    
    @Test
    void estimateTokens_WithUnknownProvider_ShouldReturnBasicEstimate() {
        // Given
        String text = "Hello world"; // 11 characters, ~3 tokens with 4 chars per token
        
        // When
        int result = tokenCountingService.estimateTokens(text, "unknown", "unknown-model");
        
        // Then
        // Expected: ceil(11/4) = 3
        assertEquals(3, result);
    }
    
    @Test
    void calculateCost_WithOpenAIGPT35_ShouldCalculateCorrectCost() {
        // Given
        String providerId = "openai";
        String model = "gpt-3.5-turbo";
        int inputTokens = 1000;
        int outputTokens = 500;
        
        // When
        double result = tokenCountingService.calculateCost(providerId, model, inputTokens, outputTokens);
        
        // Then
        // Expected: (1000/1000 * 0.0015) + (500/1000 * 0.002) = 0.0015 + 0.001 = 0.0025
        assertEquals(0.0025, result, 0.0001);
    }
    
    @Test
    void calculateCost_WithAnthropicClaude3Sonnet_ShouldCalculateCorrectCost() {
        // Given
        String providerId = "anthropic";
        String model = "claude-3-sonnet";
        int inputTokens = 1000;
        int outputTokens = 500;
        
        // When
        double result = tokenCountingService.calculateCost(providerId, model, inputTokens, outputTokens);
        
        // Then
        // Expected: (1000/1000 * 0.003) + (500/1000 * 0.015) = 0.003 + 0.0075 = 0.0105
        assertEquals(0.0105, result, 0.0001);
    }
    
    @Test
    void calculateCost_WithOllamaModel_ShouldReturnZeroCost() {
        // Given
        String providerId = "ollama";
        String model = "llama2";
        int inputTokens = 1000;
        int outputTokens = 500;
        
        // When
        double result = tokenCountingService.calculateCost(providerId, model, inputTokens, outputTokens);
        
        // Then
        assertEquals(0.0, result);
    }
    
    @Test
    void calculateCost_WithNullProvider_ShouldReturnZero() {
        // When
        double result = tokenCountingService.calculateCost(null, "gpt-3.5-turbo", 1000, 500);
        
        // Then
        assertEquals(0.0, result);
    }
    
    @Test
    void calculateCost_WithNullModel_ShouldReturnZero() {
        // When
        double result = tokenCountingService.calculateCost("openai", null, 1000, 500);
        
        // Then
        assertEquals(0.0, result);
    }
    
    @Test
    void calculateCost_WithUnknownProvider_ShouldReturnZero() {
        // When
        double result = tokenCountingService.calculateCost("unknown", "unknown-model", 1000, 500);
        
        // Then
        assertEquals(0.0, result);
    }
    
    @Test
    void calculateCost_WithUnknownModel_ShouldReturnZero() {
        // When
        double result = tokenCountingService.calculateCost("openai", "unknown-model", 1000, 500);
        
        // Then
        assertEquals(0.0, result);
    }
    
    @Test
    void getInputCostPer1K_WithValidProviderAndModel_ShouldReturnCorrectCost() {
        // When
        double result = tokenCountingService.getInputCostPer1K("openai", "gpt-4");
        
        // Then
        assertEquals(0.03, result);
    }
    
    @Test
    void getOutputCostPer1K_WithValidProviderAndModel_ShouldReturnCorrectCost() {
        // When
        double result = tokenCountingService.getOutputCostPer1K("anthropic", "claude-3-opus");
        
        // Then
        assertEquals(0.075, result);
    }
    
    @Test
    void getInputCostPer1K_WithInvalidProvider_ShouldReturnZero() {
        // When
        double result = tokenCountingService.getInputCostPer1K("invalid", "gpt-4");
        
        // Then
        assertEquals(0.0, result);
    }
    
    @Test
    void getOutputCostPer1K_WithInvalidModel_ShouldReturnZero() {
        // When
        double result = tokenCountingService.getOutputCostPer1K("openai", "invalid-model");
        
        // Then
        assertEquals(0.0, result);
    }
    
    @Test
    void isCostCalculationSupported_WithSupportedProviderAndModel_ShouldReturnTrue() {
        // When
        boolean result = tokenCountingService.isCostCalculationSupported("openai", "gpt-3.5-turbo");
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void isCostCalculationSupported_WithUnsupportedProvider_ShouldReturnFalse() {
        // When
        boolean result = tokenCountingService.isCostCalculationSupported("unsupported", "gpt-3.5-turbo");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void isCostCalculationSupported_WithUnsupportedModel_ShouldReturnFalse() {
        // When
        boolean result = tokenCountingService.isCostCalculationSupported("openai", "unsupported-model");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void calculateCost_WithLargeTokenCounts_ShouldHandleCorrectly() {
        // Given
        String providerId = "openai";
        String model = "gpt-4";
        int inputTokens = 100000; // 100K tokens
        int outputTokens = 50000;  // 50K tokens
        
        // When
        double result = tokenCountingService.calculateCost(providerId, model, inputTokens, outputTokens);
        
        // Then
        // Expected: (100000/1000 * 0.03) + (50000/1000 * 0.06) = 3.0 + 3.0 = 6.0
        assertEquals(6.0, result, 0.001);
    }
}