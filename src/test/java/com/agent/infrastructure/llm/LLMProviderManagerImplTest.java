package com.agent.infrastructure.llm;

import com.agent.domain.model.*;
import com.agent.infrastructure.llm.providers.OpenAIProvider;
import com.agent.infrastructure.llm.providers.AnthropicProvider;
import com.agent.infrastructure.llm.providers.OllamaProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LLMProviderManagerImplTest {
    
    private LLMProviderManagerImpl providerManager;
    
    @Mock
    private LLMProvider mockProvider1;
    
    @Mock
    private LLMProvider mockProvider2;
    
    @BeforeEach
    void setUp() {
        providerManager = new LLMProviderManagerImpl();
    }
    
    @Test
    void testRegisterProvider() {
        when(mockProvider1.getProviderId()).thenReturn("test-provider-1");
        
        providerManager.registerProvider(mockProvider1);
        
        List<String> availableProviders = providerManager.getAvailableProviders();
        assertTrue(availableProviders.contains("test-provider-1"));
    }
    
    @Test
    void testUnregisterProvider() {
        when(mockProvider1.getProviderId()).thenReturn("test-provider-1");
        
        providerManager.registerProvider(mockProvider1);
        providerManager.unregisterProvider("test-provider-1");
        
        List<String> availableProviders = providerManager.getAvailableProviders();
        assertFalse(availableProviders.contains("test-provider-1"));
    }
    
    @Test
    void testGenerateResponseWithSpecificProvider() {
        when(mockProvider1.getProviderId()).thenReturn("test-provider-1");
        
        LLMRequest request = new LLMRequest("Test prompt", "test-model");
        LLMResponse expectedResponse = new LLMResponse("Test response", "test-model", "test-provider-1");
        
        when(mockProvider1.generateResponse(request)).thenReturn(expectedResponse);
        
        providerManager.registerProvider(mockProvider1);
        
        LLMResponse response = providerManager.generateResponse(request, "test-provider-1");
        
        assertEquals(expectedResponse, response);
        verify(mockProvider1).generateResponse(request);
    }
    
    @Test
    void testGenerateResponseWithFailover() {
        when(mockProvider1.getProviderId()).thenReturn("provider-1");
        when(mockProvider2.getProviderId()).thenReturn("provider-2");
        when(mockProvider1.supportsModel(any())).thenReturn(true);
        when(mockProvider2.supportsModel(any())).thenReturn(true);
        
        LLMRequest request = new LLMRequest("Test prompt", "test-model");
        LLMResponse expectedResponse = new LLMResponse("Test response", "test-model", "provider-2");
        
        // First provider fails, second succeeds
        when(mockProvider1.generateResponse(request)).thenThrow(new LLMProviderException("Provider 1 failed"));
        when(mockProvider2.generateResponse(request)).thenReturn(expectedResponse);
        
        providerManager.registerProvider(mockProvider1);
        providerManager.registerProvider(mockProvider2);
        
        LLMResponse response = providerManager.generateResponse(request, null);
        
        assertEquals(expectedResponse, response);
        verify(mockProvider1).generateResponse(request);
        verify(mockProvider2).generateResponse(request);
    }
    
    @Test
    void testGenerateResponseProviderNotFound() {
        LLMRequest request = new LLMRequest("Test prompt", "test-model");
        
        assertThrows(LLMProviderException.class, () -> {
            providerManager.generateResponse(request, "non-existent-provider");
        });
    }
    
    @Test
    void testCheckProviderHealth() {
        when(mockProvider1.getProviderId()).thenReturn("test-provider-1");
        
        ProviderHealth expectedHealth = new ProviderHealth("test-provider-1", ProviderHealth.HealthStatus.HEALTHY);
        when(mockProvider1.checkHealth()).thenReturn(expectedHealth);
        
        providerManager.registerProvider(mockProvider1);
        
        ProviderHealth health = providerManager.checkProviderHealth("test-provider-1");
        
        assertEquals(expectedHealth, health);
        verify(mockProvider1).checkHealth();
    }
    
    @Test
    void testCheckProviderHealthNotFound() {
        ProviderHealth health = providerManager.checkProviderHealth("non-existent-provider");
        
        assertEquals(ProviderHealth.HealthStatus.UNKNOWN, health.getStatus());
        assertEquals("Provider not found", health.getMessage());
    }
    
    @Test
    void testCheckAllProvidersHealth() {
        when(mockProvider1.getProviderId()).thenReturn("provider-1");
        when(mockProvider2.getProviderId()).thenReturn("provider-2");
        
        ProviderHealth health1 = new ProviderHealth("provider-1", ProviderHealth.HealthStatus.HEALTHY);
        ProviderHealth health2 = new ProviderHealth("provider-2", ProviderHealth.HealthStatus.DEGRADED);
        
        when(mockProvider1.checkHealth()).thenReturn(health1);
        when(mockProvider2.checkHealth()).thenReturn(health2);
        
        providerManager.registerProvider(mockProvider1);
        providerManager.registerProvider(mockProvider2);
        
        Map<String, ProviderHealth> healthMap = providerManager.checkAllProvidersHealth();
        
        assertEquals(2, healthMap.size());
        assertEquals(health1, healthMap.get("provider-1"));
        assertEquals(health2, healthMap.get("provider-2"));
    }
}