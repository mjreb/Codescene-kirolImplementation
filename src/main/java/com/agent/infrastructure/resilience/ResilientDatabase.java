package com.agent.infrastructure.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply database resilience patterns.
 * Includes circuit breaker and retry.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResilientDatabase {
    
    /**
     * Operation type for monitoring and logging.
     */
    String operation() default "";
}