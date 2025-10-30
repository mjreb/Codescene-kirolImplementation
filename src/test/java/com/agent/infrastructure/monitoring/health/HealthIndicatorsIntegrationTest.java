package com.agent.infrastructure.monitoring.health;

import com.agent.domain.interfaces.LLMProviderManager;
import com.agent.domain.interfaces.MemoryManager;
import com.agent.domain.interfaces.TokenMonitor;
import com.agent.domain.interfaces.ToolFramework;
import com.agent.domain.model.ProviderHealth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
class HealthIndicatorsIntegrationTest {
    
    @MockBean
    private LLMProviderManager llmProviderManager;
    
    @MockBean
    private MemoryManager memoryManager;
    
    @MockBean
    private TokenMonitor tokenMonitor;
    
    @MockBean
    private ToolFramework toolFramework;
    
    @MockBean
    private RedisConnectionFactory redisConnectionFactory;
    
    @MockBean
    private RedisConnection redisConnection;
    
    @Test
    void testLLMProviderHealthIndicator_Healthy() {
        // Given
        when(llmProviderManager.getAvailableProviders()).thenReturn(List.of("openai", "anthropic"));
        ProviderHealth openaiHealth = new ProviderHealth("openai", ProviderHealth.HealthStatus.HEALTHY);
        openaiHealth.setResponseTimeMs(150);
        openaiHealth.setLastChecked(Instant.now());
        
        ProviderHealth anthropicHealth = new ProviderHealth("anthropic", ProviderHealth.HealthStatus.HEALTHY);
        anthropicHealth.setResponseTimeMs(200);
        anthropicHealth.setLastChecked(Instant.now());
        
        when(llmProviderManager.checkProviderHealth("openai")).thenReturn(openaiHealth);
        when(llmProviderManager.checkProviderHealth("anthropic")).thenReturn(anthropicHealth);
        
        LLMProviderHealthIndicator indicator = new LLMProviderHealthIndicator(llmProviderManager);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("providers"));
        assertEquals(2, health.getDetails().get("totalProviders"));
        assertEquals(2, health.getDetails().get("healthyProviders"));
    }
    
    @Test
    void testLLMProviderHealthIndicator_Unhealthy() {
        // Given
        when(llmProviderManager.getAvailableProviders()).thenReturn(List.of("openai"));
        ProviderHealth unhealthyHealth = new ProviderHealth("openai", ProviderHealth.HealthStatus.UNHEALTHY);
        unhealthyHealth.setResponseTimeMs(0);
        unhealthyHealth.setLastChecked(Instant.now());
        unhealthyHealth.setMessage("Connection timeout");
        
        when(llmProviderManager.checkProviderHealth("openai")).thenReturn(unhealthyHealth);
        
        LLMProviderHealthIndicator indicator = new LLMProviderHealthIndicator(llmProviderManager);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(0, health.getDetails().get("healthyProviders"));
    }
    
    @Test
    void testLLMProviderHealthIndicator_NoProviders() {
        // Given
        when(llmProviderManager.getAvailableProviders()).thenReturn(List.of());
        
        LLMProviderHealthIndicator indicator = new LLMProviderHealthIndicator(llmProviderManager);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("No LLM providers available", health.getDetails().get("reason"));
    }
    
    @Test
    void testMemoryHealthIndicator_Healthy() throws Exception {
        // Given
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        doNothing().when(memoryManager).storeLongTermMemory(anyString(), any(), any());
        when(memoryManager.retrieveLongTermMemory(anyString())).thenReturn(Optional.of("test-value"));
        
        MemoryHealthIndicator indicator = new MemoryHealthIndicator(memoryManager, redisConnectionFactory);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("redis"));
        assertTrue(health.getDetails().containsKey("memoryManager"));
    }
    
    @Test
    void testMemoryHealthIndicator_RedisDown() throws Exception {
        // Given
        when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Redis connection failed"));
        doNothing().when(memoryManager).storeLongTermMemory(anyString(), any(), any());
        when(memoryManager.retrieveLongTermMemory(anyString())).thenReturn(Optional.of("test-value"));
        
        MemoryHealthIndicator indicator = new MemoryHealthIndicator(memoryManager, redisConnectionFactory);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.UP, health.getStatus()); // Still UP because memory manager works
        assertTrue(health.getDetails().containsKey("redis"));
        assertTrue(health.getDetails().containsKey("memoryManager"));
    }
    
    @Test
    void testTokenMonitorHealthIndicator_Healthy() {
        // Given
        when(tokenMonitor.trackTokenUsage(anyString(), anyInt(), anyInt())).thenReturn(mock(com.agent.domain.model.TokenUsage.class));
        when(tokenMonitor.checkTokenLimit(anyString(), anyInt())).thenReturn(true);
        when(tokenMonitor.generateUsageReport(anyString(), any())).thenReturn(mock(com.agent.domain.model.UsageReport.class));
        
        TokenMonitorHealthIndicator indicator = new TokenMonitorHealthIndicator(tokenMonitor);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("tokenMonitor"));
    }
    
    @Test
    void testTokenMonitorHealthIndicator_Unhealthy() {
        // Given
        when(tokenMonitor.trackTokenUsage(anyString(), anyInt(), anyInt())).thenThrow(new RuntimeException("Token tracking failed"));
        
        TokenMonitorHealthIndicator indicator = new TokenMonitorHealthIndicator(tokenMonitor);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("tokenMonitor"));
    }
    
    @Test
    void testToolFrameworkHealthIndicator_Healthy() {
        // Given
        when(toolFramework.getAvailableTools()).thenReturn(List.of(
                mock(com.agent.domain.model.ToolDefinition.class),
                mock(com.agent.domain.model.ToolDefinition.class)
        ));
        when(toolFramework.executeTool(eq("calculator"), any())).thenReturn(
                mock(com.agent.domain.model.ToolResult.class));
        
        // Mock the tool result to return success
        var mockResult = mock(com.agent.domain.model.ToolResult.class);
        when(mockResult.isSuccess()).thenReturn(true);
        when(toolFramework.executeTool(eq("calculator"), any())).thenReturn(mockResult);
        
        ToolFrameworkHealthIndicator indicator = new ToolFrameworkHealthIndicator(toolFramework);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("toolFramework"));
        assertEquals(2, ((java.util.Map<?, ?>) health.getDetails().get("toolFramework")).get("availableTools"));
    }
    
    @Test
    void testToolFrameworkHealthIndicator_Exception() {
        // Given
        when(toolFramework.getAvailableTools()).thenThrow(new RuntimeException("Tool framework error"));
        
        ToolFrameworkHealthIndicator indicator = new ToolFrameworkHealthIndicator(toolFramework);
        
        // When
        Health health = indicator.health();
        
        // Then
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("toolFramework"));
    }
}