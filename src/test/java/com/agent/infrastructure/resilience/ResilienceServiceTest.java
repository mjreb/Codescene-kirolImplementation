package com.agent.infrastructure.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilienceServiceTest {
    
    @Mock private CircuitBreaker llmProviderCircuitBreaker;
    @Mock private CircuitBreaker externalServiceCircuitBreaker;
    @Mock private CircuitBreaker databaseCircuitBreaker;
    @Mock private Retry llmProviderRetry;
    @Mock private Retry externalServiceRetry;
    @Mock private Retry databaseRetry;
    @Mock private Bulkhead llmProviderBulkhead;
    @Mock private Bulkhead toolExecutionBulkhead;
    @Mock private Bulkhead memoryOperationsBulkhead;
    @Mock private TimeLimiter llmProviderTimeLimiter;
    @Mock private TimeLimiter toolExecutionTimeLimiter;
    @Mock private TimeLimiter externalServiceTimeLimiter;
    
    private ResilienceService resilienceService;
    
    @BeforeEach
    void setUp() {
        resilienceService = new ResilienceService(
                llmProviderCircuitBreaker, externalServiceCircuitBreaker, databaseCircuitBreaker,
                llmProviderRetry, externalServiceRetry, databaseRetry,
                llmProviderBulkhead, toolExecutionBulkhead, memoryOperationsBulkhead,
                llmProviderTimeLimiter, toolExecutionTimeLimiter, externalServiceTimeLimiter
        );
    }
    
    @Test
    void testExecuteLLMProviderCall_Success() {
        // Given
        Supplier<String> supplier = () -> "success";
        
        // When
        String result = resilienceService.executeLLMProviderCall(supplier);
        
        // Then
        assertEquals("success", result);
    }
    
    @Test
    void testExecuteLLMProviderCall_WithFallback() {
        // Given
        Supplier<String> supplier = () -> {
            throw new RuntimeException("LLM provider failed");
        };
        Supplier<String> fallback = () -> "fallback response";
        
        // When
        String result = resilienceService.executeLLMProviderCall(supplier, fallback);
        
        // Then
        assertEquals("fallback response", result);
    }
    
    @Test
    void testExecuteLLMProviderCall_NoFallback_ThrowsException() {
        // Given
        Supplier<String> supplier = () -> {
            throw new RuntimeException("LLM provider failed");
        };
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            resilienceService.executeLLMProviderCall(supplier);
        });
    }
    
    @Test
    void testExecuteLLMProviderCallAsync_Success() {
        // Given
        Supplier<String> supplier = () -> "async success";
        
        // When
        CompletableFuture<String> future = resilienceService.executeLLMProviderCallAsync(supplier);
        
        // Then
        assertDoesNotThrow(() -> {
            String result = future.get();
            assertEquals("async success", result);
        });
    }
    
    @Test
    void testExecuteLLMProviderCallAsync_WithFallback() {
        // Given
        Supplier<String> supplier = () -> {
            throw new RuntimeException("Async LLM provider failed");
        };
        Supplier<String> fallback = () -> "async fallback";
        
        // When
        CompletableFuture<String> future = resilienceService.executeLLMProviderCallAsync(supplier, fallback);
        
        // Then
        assertDoesNotThrow(() -> {
            String result = future.get();
            assertEquals("async fallback", result);
        });
    }
    
    @Test
    void testExecuteToolCall_Success() {
        // Given
        Supplier<String> supplier = () -> "tool result";
        
        // When
        String result = resilienceService.executeToolCall(supplier);
        
        // Then
        assertEquals("tool result", result);
    }
    
    @Test
    void testExecuteToolCallAsync_Success() {
        // Given
        Supplier<String> supplier = () -> "async tool result";
        
        // When
        CompletableFuture<String> future = resilienceService.executeToolCallAsync(supplier);
        
        // Then
        assertDoesNotThrow(() -> {
            String result = future.get();
            assertEquals("async tool result", result);
        });
    }
    
    @Test
    void testExecuteExternalServiceCall_Success() {
        // Given
        Supplier<String> supplier = () -> "external service result";
        
        // When
        String result = resilienceService.executeExternalServiceCall(supplier);
        
        // Then
        assertEquals("external service result", result);
    }
    
    @Test
    void testExecuteExternalServiceCall_WithFallback() {
        // Given
        Supplier<String> supplier = () -> {
            throw new RuntimeException("External service failed");
        };
        Supplier<String> fallback = () -> "external fallback";
        
        // When
        String result = resilienceService.executeExternalServiceCall(supplier, fallback);
        
        // Then
        assertEquals("external fallback", result);
    }
    
    @Test
    void testExecuteDatabaseOperation_Success() {
        // Given
        Supplier<String> supplier = () -> "database result";
        
        // When
        String result = resilienceService.executeDatabaseOperation(supplier);
        
        // Then
        assertEquals("database result", result);
    }
    
    @Test
    void testExecuteMemoryOperation_Success() {
        // Given
        Supplier<String> supplier = () -> "memory result";
        
        // When
        String result = resilienceService.executeMemoryOperation(supplier);
        
        // Then
        assertEquals("memory result", result);
    }
    
    @Test
    void testGetCircuitBreakerStatus() {
        // Given
        when(llmProviderCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(externalServiceCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);
        when(databaseCircuitBreaker.getState()).thenReturn(CircuitBreaker.State.HALF_OPEN);
        
        // When
        ResilienceService.CircuitBreakerStatus status = resilienceService.getCircuitBreakerStatus();
        
        // Then
        assertEquals(CircuitBreaker.State.CLOSED, status.getLlmProviderState());
        assertEquals(CircuitBreaker.State.OPEN, status.getExternalServiceState());
        assertEquals(CircuitBreaker.State.HALF_OPEN, status.getDatabaseState());
        assertTrue(status.isAnyCircuitOpen());
    }
}