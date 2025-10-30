package com.agent.infrastructure.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Service that provides easy access to resilience patterns.
 * Combines circuit breakers, retries, bulkheads, and time limiters.
 */
@Service
public class ResilienceService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilienceService.class);
    
    private final CircuitBreaker llmProviderCircuitBreaker;
    private final CircuitBreaker externalServiceCircuitBreaker;
    private final CircuitBreaker databaseCircuitBreaker;
    
    private final Retry llmProviderRetry;
    private final Retry externalServiceRetry;
    private final Retry databaseRetry;
    
    private final Bulkhead llmProviderBulkhead;
    private final Bulkhead toolExecutionBulkhead;
    private final Bulkhead memoryOperationsBulkhead;
    
    private final TimeLimiter llmProviderTimeLimiter;
    private final TimeLimiter toolExecutionTimeLimiter;
    private final TimeLimiter externalServiceTimeLimiter;
    
    private final ScheduledExecutorService executor;
    
    public ResilienceService(
            @Qualifier("llmProviderCircuitBreaker") CircuitBreaker llmProviderCircuitBreaker,
            @Qualifier("externalServiceCircuitBreaker") CircuitBreaker externalServiceCircuitBreaker,
            @Qualifier("databaseCircuitBreaker") CircuitBreaker databaseCircuitBreaker,
            @Qualifier("llmProviderRetry") Retry llmProviderRetry,
            @Qualifier("externalServiceRetry") Retry externalServiceRetry,
            @Qualifier("databaseRetry") Retry databaseRetry,
            @Qualifier("llmProviderBulkhead") Bulkhead llmProviderBulkhead,
            @Qualifier("toolExecutionBulkhead") Bulkhead toolExecutionBulkhead,
            @Qualifier("memoryOperationsBulkhead") Bulkhead memoryOperationsBulkhead,
            @Qualifier("llmProviderTimeLimiter") TimeLimiter llmProviderTimeLimiter,
            @Qualifier("toolExecutionTimeLimiter") TimeLimiter toolExecutionTimeLimiter,
            @Qualifier("externalServiceTimeLimiter") TimeLimiter externalServiceTimeLimiter) {
        
        this.llmProviderCircuitBreaker = llmProviderCircuitBreaker;
        this.externalServiceCircuitBreaker = externalServiceCircuitBreaker;
        this.databaseCircuitBreaker = databaseCircuitBreaker;
        this.llmProviderRetry = llmProviderRetry;
        this.externalServiceRetry = externalServiceRetry;
        this.databaseRetry = databaseRetry;
        this.llmProviderBulkhead = llmProviderBulkhead;
        this.toolExecutionBulkhead = toolExecutionBulkhead;
        this.memoryOperationsBulkhead = memoryOperationsBulkhead;
        this.llmProviderTimeLimiter = llmProviderTimeLimiter;
        this.toolExecutionTimeLimiter = toolExecutionTimeLimiter;
        this.externalServiceTimeLimiter = externalServiceTimeLimiter;
        this.executor = Executors.newScheduledThreadPool(10);
    }
    
    // ========== LLM Provider Resilience ==========
    
    /**
     * Execute LLM provider call with full resilience patterns.
     */
    public <T> T executeLLMProviderCall(Supplier<T> supplier) {
        return executeLLMProviderCall(supplier, null);
    }
    
    /**
     * Execute LLM provider call with full resilience patterns and fallback.
     */
    public <T> T executeLLMProviderCall(Supplier<T> supplier, Supplier<T> fallback) {
        logger.debug("Executing LLM provider call with resilience patterns");
        
        Supplier<T> decoratedSupplier = Bulkhead.decorateSupplier(llmProviderBulkhead, supplier);
        decoratedSupplier = CircuitBreaker.decorateSupplier(llmProviderCircuitBreaker, decoratedSupplier);
        decoratedSupplier = Retry.decorateSupplier(llmProviderRetry, decoratedSupplier);
        
        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            logger.warn("LLM provider call failed, attempting fallback", e);
            if (fallback != null) {
                return fallback.get();
            }
            throw e;
        }
    }
    
    /**
     * Execute async LLM provider call with time limiter.
     */
    public <T> CompletableFuture<T> executeLLMProviderCallAsync(Supplier<T> supplier) {
        return executeLLMProviderCallAsync(supplier, null);
    }
    
    /**
     * Execute async LLM provider call with time limiter and fallback.
     */
    public <T> CompletableFuture<T> executeLLMProviderCallAsync(Supplier<T> supplier, Supplier<T> fallback) {
        logger.debug("Executing async LLM provider call with resilience patterns");
        
        Supplier<T> decoratedSupplier = Bulkhead.decorateSupplier(llmProviderBulkhead, supplier);
        decoratedSupplier = CircuitBreaker.decorateSupplier(llmProviderCircuitBreaker, decoratedSupplier);
        decoratedSupplier = Retry.decorateSupplier(llmProviderRetry, decoratedSupplier);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(decoratedSupplier, executor);
        return TimeLimiter.decorateCompletionStage(llmProviderTimeLimiter, executor, () -> future)
                .get().toCompletableFuture()
                .exceptionally(throwable -> {
                    logger.warn("Async LLM provider call failed, attempting fallback", throwable);
                    if (fallback != null) {
                        return fallback.get();
                    }
                    throw new RuntimeException(throwable);
                });
    }
    
    // ========== Tool Execution Resilience ==========
    
    /**
     * Execute tool with bulkhead and time limiter.
     */
    public <T> T executeToolCall(Supplier<T> supplier) {
        logger.debug("Executing tool call with resilience patterns");
        
        Supplier<T> decoratedSupplier = Bulkhead.decorateSupplier(toolExecutionBulkhead, supplier);
        
        return decoratedSupplier.get();
    }
    
    /**
     * Execute async tool call with time limiter.
     */
    public <T> CompletableFuture<T> executeToolCallAsync(Supplier<T> supplier) {
        logger.debug("Executing async tool call with resilience patterns");
        
        Supplier<T> decoratedSupplier = Bulkhead.decorateSupplier(toolExecutionBulkhead, supplier);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(decoratedSupplier, executor);
        return TimeLimiter.decorateCompletionStage(toolExecutionTimeLimiter, executor, () -> future)
                .get().toCompletableFuture();
    }
    
    // ========== External Service Resilience ==========
    
    /**
     * Execute external service call with circuit breaker and retry.
     */
    public <T> T executeExternalServiceCall(Supplier<T> supplier) {
        return executeExternalServiceCall(supplier, null);
    }
    
    /**
     * Execute external service call with circuit breaker, retry, and fallback.
     */
    public <T> T executeExternalServiceCall(Supplier<T> supplier, Supplier<T> fallback) {
        logger.debug("Executing external service call with resilience patterns");
        
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(externalServiceCircuitBreaker, supplier);
        decoratedSupplier = Retry.decorateSupplier(externalServiceRetry, decoratedSupplier);
        
        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            logger.warn("External service call failed, attempting fallback", e);
            if (fallback != null) {
                return fallback.get();
            }
            throw e;
        }
    }
    
    /**
     * Execute async external service call with time limiter.
     */
    public <T> CompletableFuture<T> executeExternalServiceCallAsync(Supplier<T> supplier, Supplier<T> fallback) {
        logger.debug("Executing async external service call with resilience patterns");
        
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(externalServiceCircuitBreaker, supplier);
        decoratedSupplier = Retry.decorateSupplier(externalServiceRetry, decoratedSupplier);
        
        CompletableFuture<T> future = CompletableFuture.supplyAsync(decoratedSupplier, executor);
        return TimeLimiter.decorateCompletionStage(externalServiceTimeLimiter, executor, () -> future)
                .get().toCompletableFuture()
                .exceptionally(throwable -> {
                    logger.warn("Async external service call failed, attempting fallback", throwable);
                    if (fallback != null) {
                        return fallback.get();
                    }
                    throw new RuntimeException(throwable);
                });
    }
    
    // ========== Database Resilience ==========
    
    /**
     * Execute database operation with circuit breaker and retry.
     */
    public <T> T executeDatabaseOperation(Supplier<T> supplier) {
        logger.debug("Executing database operation with resilience patterns");
        
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(databaseCircuitBreaker, supplier);
        decoratedSupplier = Retry.decorateSupplier(databaseRetry, decoratedSupplier);
        
        return decoratedSupplier.get();
    }
    
    // ========== Memory Operations Resilience ==========
    
    /**
     * Execute memory operation with bulkhead.
     */
    public <T> T executeMemoryOperation(Supplier<T> supplier) {
        logger.debug("Executing memory operation with resilience patterns");
        
        Supplier<T> decoratedSupplier = Bulkhead.decorateSupplier(memoryOperationsBulkhead, supplier);
        
        return decoratedSupplier.get();
    }
    
    // ========== Circuit Breaker Status ==========
    
    /**
     * Get circuit breaker states for monitoring.
     */
    public CircuitBreakerStatus getCircuitBreakerStatus() {
        return new CircuitBreakerStatus(
                llmProviderCircuitBreaker.getState(),
                externalServiceCircuitBreaker.getState(),
                databaseCircuitBreaker.getState()
        );
    }
    
    /**
     * Circuit breaker status information.
     */
    public static class CircuitBreakerStatus {
        private final CircuitBreaker.State llmProviderState;
        private final CircuitBreaker.State externalServiceState;
        private final CircuitBreaker.State databaseState;
        
        public CircuitBreakerStatus(CircuitBreaker.State llmProviderState, 
                                  CircuitBreaker.State externalServiceState, 
                                  CircuitBreaker.State databaseState) {
            this.llmProviderState = llmProviderState;
            this.externalServiceState = externalServiceState;
            this.databaseState = databaseState;
        }
        
        public CircuitBreaker.State getLlmProviderState() { return llmProviderState; }
        public CircuitBreaker.State getExternalServiceState() { return externalServiceState; }
        public CircuitBreaker.State getDatabaseState() { return databaseState; }
        
        public boolean isAnyCircuitOpen() {
            return llmProviderState == CircuitBreaker.State.OPEN ||
                   externalServiceState == CircuitBreaker.State.OPEN ||
                   databaseState == CircuitBreaker.State.OPEN;
        }
    }
}