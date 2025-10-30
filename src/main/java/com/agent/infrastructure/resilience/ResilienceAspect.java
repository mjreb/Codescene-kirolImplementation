package com.agent.infrastructure.resilience;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect for applying resilience patterns using annotations.
 */
@Aspect
@Component
public class ResilienceAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilienceAspect.class);
    
    private final ResilienceService resilienceService;
    
    public ResilienceAspect(ResilienceService resilienceService) {
        this.resilienceService = resilienceService;
    }
    
    /**
     * Apply LLM provider resilience patterns.
     */
    @Around("@annotation(resilientLLMProvider)")
    public Object applyLLMProviderResilience(ProceedingJoinPoint joinPoint, ResilientLLMProvider resilientLLMProvider) throws Throwable {
        logger.debug("Applying LLM provider resilience to method: {}", joinPoint.getSignature().getName());
        
        return resilienceService.executeLLMProviderCall(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }
    
    /**
     * Apply tool execution resilience patterns.
     */
    @Around("@annotation(resilientToolExecution)")
    public Object applyToolExecutionResilience(ProceedingJoinPoint joinPoint, ResilientToolExecution resilientToolExecution) throws Throwable {
        logger.debug("Applying tool execution resilience to method: {}", joinPoint.getSignature().getName());
        
        return resilienceService.executeToolCall(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }
    
    /**
     * Apply external service resilience patterns.
     */
    @Around("@annotation(resilientExternalService)")
    public Object applyExternalServiceResilience(ProceedingJoinPoint joinPoint, ResilientExternalService resilientExternalService) throws Throwable {
        logger.debug("Applying external service resilience to method: {}", joinPoint.getSignature().getName());
        
        return resilienceService.executeExternalServiceCall(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }
    
    /**
     * Apply database resilience patterns.
     */
    @Around("@annotation(resilientDatabase)")
    public Object applyDatabaseResilience(ProceedingJoinPoint joinPoint, ResilientDatabase resilientDatabase) throws Throwable {
        logger.debug("Applying database resilience to method: {}", joinPoint.getSignature().getName());
        
        return resilienceService.executeDatabaseOperation(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }
    
    /**
     * Apply memory operation resilience patterns.
     */
    @Around("@annotation(resilientMemoryOperation)")
    public Object applyMemoryOperationResilience(ProceedingJoinPoint joinPoint, ResilientMemoryOperation resilientMemoryOperation) throws Throwable {
        logger.debug("Applying memory operation resilience to method: {}", joinPoint.getSignature().getName());
        
        return resilienceService.executeMemoryOperation(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }
}