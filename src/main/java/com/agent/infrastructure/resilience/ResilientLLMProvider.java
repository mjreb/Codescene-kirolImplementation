package com.agent.infrastructure.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply LLM provider resilience patterns.
 * Includes circuit breaker, retry, bulkhead, and time limiter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResilientLLMProvider {
    
    /**
     * Whether to enable fallback mechanism.
     */
    boolean enableFallback() default false;
    
    /**
     * Fallback method name (must be in the same class).
     */
    String fallbackMethod() default "";
}