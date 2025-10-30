package com.agent.infrastructure.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for resilience patterns using Resilience4j.
 * Provides circuit breakers, retry mechanisms, bulkheads, and time limiters.
 */
@Configuration
public class ResilienceConfiguration {
    
    // ========== Circuit Breaker Configurations ==========
    
    /**
     * Circuit breaker for LLM provider calls.
     */
    @Bean
    public CircuitBreaker llmProviderCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .slidingWindowSize(10) // Consider last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls to calculate failure rate
                .slowCallRateThreshold(80.0f) // Consider slow if 80% of calls are slow
                .slowCallDurationThreshold(Duration.ofSeconds(10)) // Call is slow if > 10s
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        return CircuitBreaker.of("llmProvider", config);
    }
    
    /**
     * Circuit breaker for external service calls.
     */
    @Bean
    public CircuitBreaker externalServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60.0f)
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .slidingWindowSize(8)
                .minimumNumberOfCalls(3)
                .slowCallRateThreshold(70.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        return CircuitBreaker.of("externalService", config);
    }
    
    /**
     * Circuit breaker for database operations.
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(70.0f)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .slidingWindowSize(6)
                .minimumNumberOfCalls(3)
                .slowCallRateThreshold(90.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        return CircuitBreaker.of("database", config);
    }
    
    // ========== Retry Configurations ==========
    
    /**
     * Retry configuration for LLM provider calls.
     */
    @Bean
    public Retry llmProviderRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryOnException(throwable -> {
                    // Retry on network errors, timeouts, and 5xx responses
                    return throwable instanceof java.net.SocketTimeoutException ||
                           throwable instanceof java.net.ConnectException ||
                           throwable instanceof java.io.IOException ||
                           (throwable.getMessage() != null && 
                            (throwable.getMessage().contains("5") || 
                             throwable.getMessage().contains("timeout")));
                })
                .build();
        
        return Retry.of("llmProvider", config);
    }
    
    /**
     * Retry configuration for external service calls.
     */
    @Bean
    public Retry externalServiceRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofSeconds(1))
                .retryOnException(throwable -> {
                    return throwable instanceof java.net.SocketTimeoutException ||
                           throwable instanceof java.net.ConnectException;
                })
                .build();
        
        return Retry.of("externalService", config);
    }
    
    /**
     * Retry configuration for database operations.
     */
    @Bean
    public Retry databaseRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(throwable -> {
                    // Retry on transient database errors
                    return throwable instanceof org.springframework.dao.TransientDataAccessException ||
                           throwable instanceof java.sql.SQLTransientException ||
                           (throwable.getMessage() != null && 
                            throwable.getMessage().toLowerCase().contains("connection"));
                })
                .build();
        
        return Retry.of("database", config);
    }
    
    // ========== Bulkhead Configurations ==========
    
    /**
     * Bulkhead for LLM provider calls to isolate resources.
     */
    @Bean
    public Bulkhead llmProviderBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(10) // Allow max 10 concurrent LLM calls
                .maxWaitDuration(Duration.ofSeconds(5)) // Wait max 5s for a slot
                .build();
        
        return Bulkhead.of("llmProvider", config);
    }
    
    /**
     * Bulkhead for tool execution to prevent resource exhaustion.
     */
    @Bean
    public Bulkhead toolExecutionBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(20) // Allow max 20 concurrent tool executions
                .maxWaitDuration(Duration.ofSeconds(3))
                .build();
        
        return Bulkhead.of("toolExecution", config);
    }
    
    /**
     * Bulkhead for memory operations.
     */
    @Bean
    public Bulkhead memoryOperationsBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(15) // Allow max 15 concurrent memory operations
                .maxWaitDuration(Duration.ofSeconds(2))
                .build();
        
        return Bulkhead.of("memoryOperations", config);
    }
    
    // ========== Time Limiter Configurations ==========
    
    /**
     * Time limiter for LLM provider calls.
     */
    @Bean
    public TimeLimiter llmProviderTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30)) // Timeout after 30 seconds
                .cancelRunningFuture(true)
                .build();
        
        return TimeLimiter.of("llmProvider", config);
    }
    
    /**
     * Time limiter for tool execution.
     */
    @Bean
    public TimeLimiter toolExecutionTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(60)) // Timeout after 60 seconds
                .cancelRunningFuture(true)
                .build();
        
        return TimeLimiter.of("toolExecution", config);
    }
    
    /**
     * Time limiter for external service calls.
     */
    @Bean
    public TimeLimiter externalServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10)) // Timeout after 10 seconds
                .cancelRunningFuture(true)
                .build();
        
        return TimeLimiter.of("externalService", config);
    }
}