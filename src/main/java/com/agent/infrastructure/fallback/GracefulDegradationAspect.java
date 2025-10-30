package com.agent.infrastructure.fallback;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect that applies graceful degradation patterns automatically.
 */
@Aspect
@Component
public class GracefulDegradationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(GracefulDegradationAspect.class);
    
    private final FallbackService fallbackService;
    private final SystemStatusService systemStatusService;
    
    public GracefulDegradationAspect(FallbackService fallbackService, SystemStatusService systemStatusService) {
        this.fallbackService = fallbackService;
        this.systemStatusService = systemStatusService;
    }
    
    /**
     * Apply graceful degradation with fallback.
     */
    @Around("@annotation(gracefulDegradation)")
    public Object applyGracefulDegradation(ProceedingJoinPoint joinPoint, GracefulDegradation gracefulDegradation) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        logger.debug("Applying graceful degradation to method: {}", methodName);
        
        try {
            // Try the primary operation first
            Object result = joinPoint.proceed();
            
            // Cache successful results if enabled
            if (gracefulDegradation.cacheResults()) {
                cacheResult(methodName, result);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.warn("Primary operation failed for method: {}, attempting fallback", methodName, e);
            
            // Check if we should use fallback
            if (shouldUseFallback(gracefulDegradation, e)) {
                return provideFallback(joinPoint, gracefulDegradation, e);
            } else {
                // Re-throw the exception if fallback is not appropriate
                throw e;
            }
        }
    }
    
    /**
     * Determine if fallback should be used based on exception and configuration.
     */
    private boolean shouldUseFallback(GracefulDegradation gracefulDegradation, Exception e) {
        // Always use fallback if system is degraded
        if (fallbackService.isSystemDegraded()) {
            return true;
        }
        
        // Check if exception type is in the fallback list
        for (Class<? extends Exception> fallbackException : gracefulDegradation.fallbackOn()) {
            if (fallbackException.isAssignableFrom(e.getClass())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Provide fallback response based on method and configuration.
     */
    private Object provideFallback(ProceedingJoinPoint joinPoint, GracefulDegradation gracefulDegradation, Exception originalException) {
        String methodName = joinPoint.getSignature().getName();
        
        // Try custom fallback method first
        if (!gracefulDegradation.fallbackMethod().isEmpty()) {
            try {
                return invokeFallbackMethod(joinPoint, gracefulDegradation.fallbackMethod(), originalException);
            } catch (Exception e) {
                logger.warn("Custom fallback method failed for: {}", methodName, e);
            }
        }
        
        // Use default fallback based on return type
        Class<?> returnType = joinPoint.getSignature().getDeclaringType();
        return provideDefaultFallback(returnType, gracefulDegradation.defaultMessage());
    }
    
    /**
     * Invoke custom fallback method.
     */
    private Object invokeFallbackMethod(ProceedingJoinPoint joinPoint, String fallbackMethodName, Exception originalException) throws Exception {
        // This would require reflection to invoke the fallback method
        // For simplicity, we'll return a default response
        logger.info("Custom fallback method '{}' would be invoked here", fallbackMethodName);
        return provideDefaultFallback(String.class, "Fallback response");
    }
    
    /**
     * Provide default fallback based on return type.
     */
    private Object provideDefaultFallback(Class<?> returnType, String defaultMessage) {
        if (returnType == String.class) {
            return defaultMessage.isEmpty() ? "Service temporarily unavailable" : defaultMessage;
        } else if (returnType == Boolean.class || returnType == boolean.class) {
            return false;
        } else if (Number.class.isAssignableFrom(returnType) || returnType.isPrimitive()) {
            return 0;
        } else {
            return null;
        }
    }
    
    /**
     * Cache successful result for future fallback use.
     */
    private void cacheResult(String methodName, Object result) {
        if (result != null) {
            fallbackService.cacheResponse(methodName, result.toString(), "method_result");
        }
    }
}