package com.agent.infrastructure.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply tool execution resilience patterns.
 * Includes bulkhead and time limiter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResilientToolExecution {
    
    /**
     * Maximum execution time in seconds.
     */
    int timeoutSeconds() default 60;
}