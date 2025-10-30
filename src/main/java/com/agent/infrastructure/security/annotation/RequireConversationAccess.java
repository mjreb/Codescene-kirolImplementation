package com.agent.infrastructure.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require conversation access for method execution
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireConversationAccess {
    String conversationIdParam() default "conversationId";
    boolean requireWrite() default false;
    String message() default "Access denied: insufficient conversation permissions";
}