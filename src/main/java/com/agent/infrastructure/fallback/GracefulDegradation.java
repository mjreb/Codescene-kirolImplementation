package com.agent.infrastructure.fallback;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable graceful degradation with fallback mechanisms.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GracefulDegradation {
    
    /**
     * Exception types that should trigger fallback.
     */
    Class<? extends Exception>[] fallbackOn() default {Exception.class};
    
    /**
     * Custom fallback method name (must be in the same class).
     */
    String fallbackMethod() default "";
    
    /**
     * Default fallback message.
     */
    String defaultMessage() default "";
    
    /**
     * Whether to cache successful results for future fallback use.
     */
    boolean cacheResults() default false;
    
    /**
     * Whether to enable fallback only when system is degraded.
     */
    boolean onlyWhenDegraded() default false;
}