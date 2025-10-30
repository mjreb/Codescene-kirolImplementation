package com.agent.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for circuit breaker behavior with simulated failures.
 */
class CircuitBreakerIntegrationTest {
    
    private CircuitBreaker circuitBreaker;
    
    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofMillis(100)) // Short wait for testing
                .slidingWindowSize(4) // Consider last 4 calls
                .minimumNumberOfCalls(3) // Need at least 3 calls to calculate failure rate
                .permittedNumberOfCallsInHalfOpenState(2) // Allow 2 calls in half-open state
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        circuitBreaker = CircuitBreaker.of("test", config);
    }
    
    @Test
    void testCircuitBreakerOpensOnFailures() {
        // Given
        Supplier<String> failingSupplier = () -> {
            throw new RuntimeException("Service failure");
        };
        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, failingSupplier);
        
        // Initially circuit should be closed
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        
        // When - Execute failing calls to trigger circuit opening
        for (int i = 0; i < 4; i++) {
            try {
                decoratedSupplier.get();
            } catch (Exception e) {
                // Expected failures
            }
        }
        
        // Then - Circuit should be open after enough failures
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }
    
    @Test
    void testCircuitBreakerTransitionsToHalfOpen() throws InterruptedException {
        // Given
        Supplier<String> failingSupplier = () -> {
            throw new RuntimeException("Service failure");
        };
        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, failingSupplier);
        
        // Open the circuit by causing failures
        for (int i = 0; i < 4; i++) {
            try {
                decoratedSupplier.get();
            } catch (Exception e) {
                // Expected failures
            }
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // When - Wait for automatic transition to half-open
        Thread.sleep(150); // Wait longer than waitDurationInOpenState
        
        // Try one call to trigger transition
        try {
            decoratedSupplier.get();
        } catch (Exception e) {
            // Expected
        }
        
        // Then - Circuit should be half-open
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }
    
    @Test
    void testCircuitBreakerClosesOnSuccessfulCalls() throws InterruptedException {
        // Given
        Supplier<String> failingSupplier = () -> {
            throw new RuntimeException("Service failure");
        };
        Supplier<String> successfulSupplier = () -> "success";
        
        // Open the circuit first
        Supplier<String> decoratedFailingSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, failingSupplier);
        for (int i = 0; i < 4; i++) {
            try {
                decoratedFailingSupplier.get();
            } catch (Exception e) {
                // Expected failures
            }
        }
        
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        
        // Wait for transition to half-open
        Thread.sleep(150);
        
        // When - Execute successful calls in half-open state
        Supplier<String> decoratedSuccessfulSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, successfulSupplier);
        
        // First call to transition to half-open
        try {
            decoratedFailingSupplier.get();
        } catch (Exception e) {
            // Expected
        }
        
        // Successful calls to close the circuit
        for (int i = 0; i < 2; i++) {
            String result = decoratedSuccessfulSupplier.get();
            assertEquals("success", result);
        }
        
        // Then - Circuit should be closed
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void testCircuitBreakerMetrics() {
        // Given
        Supplier<String> mixedSupplier = new MixedResultSupplier();
        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, mixedSupplier);
        
        // When - Execute mixed success/failure calls
        for (int i = 0; i < 10; i++) {
            try {
                decoratedSupplier.get();
            } catch (Exception e) {
                // Some calls will fail
            }
        }
        
        // Then - Check metrics
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertTrue(metrics.getNumberOfSuccessfulCalls() > 0);
        assertTrue(metrics.getNumberOfFailedCalls() > 0);
        assertEquals(10, metrics.getNumberOfSuccessfulCalls() + metrics.getNumberOfFailedCalls());
    }
    
    /**
     * Supplier that alternates between success and failure.
     */
    private static class MixedResultSupplier implements Supplier<String> {
        private int callCount = 0;
        
        @Override
        public String get() {
            callCount++;
            if (callCount % 2 == 0) {
                throw new RuntimeException("Simulated failure");
            }
            return "success";
        }
    }
}