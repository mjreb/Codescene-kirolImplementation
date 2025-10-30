package com.agent.infrastructure.resilience;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for retry mechanisms and backoff strategies.
 */
class RetryMechanismTest {
    
    private Retry retry;
    
    @BeforeEach
    void setUp() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10)) // Short wait for testing
                .retryOnException(throwable -> throwable instanceof RuntimeException)
                .build();
        
        retry = Retry.of("test", config);
    }
    
    @Test
    void testRetrySucceedsOnSecondAttempt() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt == 1) {
                throw new RuntimeException("First attempt fails");
            }
            return "success on attempt " + attempt;
        };
        
        Supplier<String> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        
        // When
        String result = decoratedSupplier.get();
        
        // Then
        assertEquals("success on attempt 2", result);
        assertEquals(2, attemptCount.get());
    }
    
    @Test
    void testRetryExhaustsAllAttempts() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Always fails");
        };
        
        Supplier<String> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, decoratedSupplier::get);
        assertEquals("Always fails", exception.getMessage());
        assertEquals(3, attemptCount.get()); // Should have tried 3 times
    }
    
    @Test
    void testRetryDoesNotRetryOnNonRetryableException() {
        // Given
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .retryOnException(throwable -> throwable instanceof IllegalStateException)
                .build();
        
        Retry specificRetry = Retry.of("specific", config);
        
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Non-retryable exception");
        };
        
        Supplier<String> decoratedSupplier = Retry.decorateSupplier(specificRetry, supplier);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, decoratedSupplier::get);
        assertEquals("Non-retryable exception", exception.getMessage());
        assertEquals(1, attemptCount.get()); // Should have tried only once
    }
    
    @Test
    void testExponentialBackoffTiming() {
        // Given
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .build();
        
        Retry backoffRetry = Retry.of("backoff", config);
        
        AtomicInteger attemptCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        
        Supplier<String> supplier = () -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException("Always fails");
        };
        
        Supplier<String> decoratedSupplier = Retry.decorateSupplier(backoffRetry, supplier);
        
        // When
        assertThrows(RuntimeException.class, decoratedSupplier::get);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertEquals(3, attemptCount.get());
        // Should have waited: 100ms + 200ms = 300ms minimum
        // Adding some tolerance for test execution time
        assertTrue(endTime - startTime >= 250, "Expected at least 250ms delay, got " + (endTime - startTime) + "ms");
    }
    
    @Test
    void testRetryMetrics() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 2) {
                throw new RuntimeException("Fail on attempts 1 and 2");
            }
            return "success";
        };
        
        Supplier<String> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        
        // When
        String result = decoratedSupplier.get();
        
        // Then
        assertEquals("success", result);
        
        Retry.Metrics metrics = retry.getMetrics();
        assertEquals(1, metrics.getNumberOfSuccessfulCallsWithRetryAttempt());
        assertEquals(0, metrics.getNumberOfFailedCallsWithRetryAttempt());
    }
    
    @Test
    void testRetryWithPredicate() {
        // Given
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .retryOnResult(result -> "retry".equals(result))
                .build();
        
        Retry predicateRetry = Retry.of("predicate", config);
        
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> supplier = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 2) {
                return "retry"; // This should trigger retry
            }
            return "success";
        };
        
        Supplier<String> decoratedSupplier = Retry.decorateSupplier(predicateRetry, supplier);
        
        // When
        String result = decoratedSupplier.get();
        
        // Then
        assertEquals("success", result);
        assertEquals(3, attemptCount.get());
    }
}