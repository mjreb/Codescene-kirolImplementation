package com.agent.infrastructure.security.aspect;

import com.agent.infrastructure.security.service.AuditLoggingService;
import com.agent.infrastructure.security.service.InputValidationService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect for automatic input validation
 */
@Aspect
@Component
public class InputValidationAspect {

    private final InputValidationService inputValidationService;
    private final AuditLoggingService auditLoggingService;

    public InputValidationAspect(
            InputValidationService inputValidationService,
            AuditLoggingService auditLoggingService
    ) {
        this.inputValidationService = inputValidationService;
        this.auditLoggingService = auditLoggingService;
    }

    /**
     * Validate inputs for conversation endpoints
     */
    @Before("execution(* com.agent.presentation.controller.AgentController.*(..))")
    public void validateConversationInputs(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();

        for (Object arg : args) {
            if (arg instanceof String) {
                String stringArg = (String) arg;
                
                // Validate conversation ID format
                if (methodName.contains("conversationId") || stringArg.matches("^[a-fA-F0-9-]{36}$")) {
                    InputValidationService.ValidationResult result = 
                        inputValidationService.validateConversationId(stringArg);
                    
                    if (!result.isValid()) {
                        logSecurityViolation("INVALID_CONVERSATION_ID", result.getErrorMessage());
                        throw new IllegalArgumentException("Invalid conversation ID: " + result.getErrorMessage());
                    }
                }
                
                // Check for malicious content
                if (inputValidationService.containsSqlInjection(stringArg)) {
                    logSecurityViolation("SQL_INJECTION_ATTEMPT", "SQL injection patterns detected");
                    throw new SecurityException("Invalid input: potential SQL injection detected");
                }
                
                if (inputValidationService.containsXss(stringArg)) {
                    logSecurityViolation("XSS_ATTEMPT", "XSS patterns detected");
                    throw new SecurityException("Invalid input: potential XSS detected");
                }
                
                if (inputValidationService.containsPathTraversal(stringArg)) {
                    logSecurityViolation("PATH_TRAVERSAL_ATTEMPT", "Path traversal patterns detected");
                    throw new SecurityException("Invalid input: potential path traversal detected");
                }
            }
        }
    }

    /**
     * Validate inputs for authentication endpoints
     */
    @Before("execution(* com.agent.presentation.controller.AuthController.*(..))")
    public void validateAuthInputs(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg instanceof String) {
                String stringArg = (String) arg;
                
                // Check for malicious content in auth requests
                if (inputValidationService.containsSqlInjection(stringArg)) {
                    logSecurityViolation("SQL_INJECTION_ATTEMPT", "SQL injection in auth request");
                    throw new SecurityException("Invalid input: potential SQL injection detected");
                }
                
                if (inputValidationService.containsXss(stringArg)) {
                    logSecurityViolation("XSS_ATTEMPT", "XSS in auth request");
                    throw new SecurityException("Invalid input: potential XSS detected");
                }
            }
        }
    }

    private void logSecurityViolation(String violationType, String description) {
        String ipAddress = getClientIpAddress();
        auditLoggingService.logSecurityViolation(violationType, description, ipAddress);
    }

    private String getClientIpAddress() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // Check for X-Forwarded-For header (proxy/load balancer)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            // Check for X-Real-IP header
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            // Fall back to remote address
            return request.getRemoteAddr();
        }
        
        return "unknown";
    }
}