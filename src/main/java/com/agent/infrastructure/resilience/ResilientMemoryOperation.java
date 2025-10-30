package com.agent.infrastructure.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply memory operation resilience patterns.
 * Includes bulkhead for resource isolation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResilientMemoryOperation {
    
    /**
     * Memory operation type for monitoring and logging.
     */
    String operationType() default "";
}