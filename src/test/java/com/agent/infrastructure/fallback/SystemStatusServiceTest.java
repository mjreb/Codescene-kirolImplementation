package com.agent.infrastructure.fallback;

import com.agent.infrastructure.resilience.ResilienceService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemStatusServiceTest {
    
    @Mock
    private ResilienceService resilienceService;
    
    @Mock
    private ResilienceService.CircuitBreakerStatus circuitBreakerStatus;
    
    private SystemStatusService systemStatusService;
    
    @BeforeEach
    void setUp() {
        systemStatusService = new SystemStatusService(resilienceService);
    }
    
    @Test
    void testGetCurrentStatus_AllOperational() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.CLOSED);
        
        // When
        SystemStatus status = systemStatusService.getCurrentStatus();
        
        // Then
        assertNotNull(status);
        assertTrue(status.isFullyOperational());
        assertTrue(status.isLLMProvidersOperational());
        assertTrue(status.isExternalServicesOperational());
        assertTrue(status.isDatabaseOperational());
        assertTrue(status.isToolsOperational());
    }
    
    @Test
    void testGetCurrentStatus_LLMProviderDown() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.CLOSED);
        
        // When
        SystemStatus status = systemStatusService.getCurrentStatus();
        
        // Then
        assertNotNull(status);
        assertFalse(status.isFullyOperational());
        assertFalse(status.isLLMProvidersOperational());
        assertTrue(status.isExternalServicesOperational());
        assertTrue(status.isDatabaseOperational());
        assertTrue(status.isToolsOperational());
    }
    
    @Test
    void testGetCurrentStatus_DatabaseDown() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.OPEN);
        
        // When
        SystemStatus status = systemStatusService.getCurrentStatus();
        
        // Then
        assertNotNull(status);
        assertFalse(status.isFullyOperational());
        assertTrue(status.isLLMProvidersOperational());
        assertTrue(status.isExternalServicesOperational());
        assertFalse(status.isDatabaseOperational());
        assertTrue(status.isToolsOperational());
    }
    
    @Test
    void testIsComponentOperational_LLM() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.OPEN);
        
        // When & Then
        assertTrue(systemStatusService.isComponentOperational("llm"));
        assertTrue(systemStatusService.isComponentOperational("llmprovider"));
        assertTrue(systemStatusService.isComponentOperational("external"));
        assertFalse(systemStatusService.isComponentOperational("database"));
        assertTrue(systemStatusService.isComponentOperational("tools"));
    }
    
    @Test
    void testGetDegradationLevel_FullyOperational() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.CLOSED);
        
        // When
        double degradationLevel = systemStatusService.getDegradationLevel();
        
        // Then
        assertEquals(0.0, degradationLevel, 0.01);
    }
    
    @Test
    void testGetDegradationLevel_HalfDegraded() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.CLOSED);
        
        // When
        double degradationLevel = systemStatusService.getDegradationLevel();
        
        // Then
        assertEquals(0.5, degradationLevel, 0.01); // 2 out of 4 components down
    }
    
    @Test
    void testGetDegradationLevel_FullyDegraded() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.OPEN);
        
        // When
        double degradationLevel = systemStatusService.getDegradationLevel();
        
        // Then
        assertEquals(0.75, degradationLevel, 0.01); // 3 out of 4 components down (tools assumed operational)
    }
    
    @Test
    void testGetStatusDescription_AllOperational() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.CLOSED);
        
        // When
        String description = systemStatusService.getStatusDescription();
        
        // Then
        assertEquals("All systems operational", description);
    }
    
    @Test
    void testGetStatusDescription_MinorDisruption() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.CLOSED);
        
        // When
        String description = systemStatusService.getStatusDescription();
        
        // Then
        assertEquals("Minor service disruption", description);
    }
    
    @Test
    void testGetStatusDescription_PartialDegradation() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.CLOSED);
        
        // When
        String description = systemStatusService.getStatusDescription();
        
        // Then
        assertEquals("Partial service degradation", description);
    }
    
    @Test
    void testGetStatusDescription_MajorDisruption() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.OPEN);
        
        // When
        String description = systemStatusService.getStatusDescription();
        
        // Then
        assertEquals("Major service disruption", description);
    }
    
    @Test
    void testRefreshStatus() {
        // Given
        when(resilienceService.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus);
        when(circuitBreakerStatus.getLlmProviderState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getExternalServiceState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerStatus.getDatabaseState()).thenReturn(CircuitBreaker.State.CLOSED);
        
        // When
        systemStatusService.refreshStatus();
        
        // Then
        verify(resilienceService, atLeastOnce()).getCircuitBreakerStatus();
    }
}