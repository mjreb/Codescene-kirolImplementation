package com.agent.infrastructure.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply external service resilience patterns.
 * Includes circuit breaker, retry, and time limiter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResilientExternalService {
    
    /**
     * Service name for monitoring and logging.
     */
    String serviceName() default "";
    
    /**
     * Whether to enable fallback mechanism.
     */
    boolean enableFallback() default false;
    
    /**
     * Fallback method name (must be in the same class).
     */
    String fallbackMethod() default "";
}