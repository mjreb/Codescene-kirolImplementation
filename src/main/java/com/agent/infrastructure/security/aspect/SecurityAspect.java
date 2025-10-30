package com.agent.infrastructure.security.aspect;

import com.agent.infrastructure.security.annotation.RequireConversationAccess;
import com.agent.infrastructure.security.annotation.RequirePermission;
import com.agent.infrastructure.security.service.AuthorizationService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Aspect for handling security annotations
 */
@Aspect
@Component
public class SecurityAspect {

    private final AuthorizationService authorizationService;

    public SecurityAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * Handle @RequirePermission annotation
     */
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        if (!authorizationService.hasPermission(requirePermission.value())) {
            throw new AccessDeniedException(requirePermission.message());
        }
    }

    /**
     * Handle @RequireConversationAccess annotation
     */
    @Before("@annotation(requireConversationAccess)")
    public void checkConversationAccess(JoinPoint joinPoint, RequireConversationAccess requireConversationAccess) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        String conversationId = null;
        String conversationIdParam = requireConversationAccess.conversationIdParam();

        // Find conversation ID parameter
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(conversationIdParam)) {
                conversationId = (String) args[i];
                break;
            }
        }

        if (conversationId == null) {
            throw new AccessDeniedException("Conversation ID not found in method parameters");
        }

        // Get current user ID
        String currentUserId = authorizationService.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("User not authenticated");
        }

        // Check access based on requirement
        boolean hasAccess;
        if (requireConversationAccess.requireWrite()) {
            hasAccess = authorizationService.canModifyConversation(conversationId, currentUserId);
        } else {
            hasAccess = authorizationService.canAccessConversation(conversationId, currentUserId);
        }

        if (!hasAccess) {
            throw new AccessDeniedException(requireConversationAccess.message());
        }
    }
}