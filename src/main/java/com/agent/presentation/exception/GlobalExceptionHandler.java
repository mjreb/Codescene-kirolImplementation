package com.agent.presentation.exception;

import com.agent.domain.exception.*;
import com.agent.presentation.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API error responses.
 * Provides comprehensive error handling with proper HTTP status codes and user-friendly messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors from request DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = new ErrorResponse(
                "Validation Failed",
                "Request validation failed",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );
        errorResponse.setDetails(details);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "Bad Request",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle security exceptions (access denied).
     */
    @ExceptionHandler(java.lang.SecurityException.class)
    public ResponseEntity<ErrorResponse> handleJavaSecurityException(
            java.lang.SecurityException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "Access Denied",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle illegal state exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "Invalid State",
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle generic runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "Internal Server Error",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    // ========== Agent Exception Handlers ==========
    
    /**
     * Handle LLM provider exceptions.
     */
    @ExceptionHandler(LLMProviderException.class)
    public ResponseEntity<ErrorResponse> handleLLMProviderException(
            LLMProviderException ex, HttpServletRequest request) {
        
        logger.warn("LLM Provider error: {}", ex.getTechnicalDetails(), ex);
        
        HttpStatus status = determineHttpStatus(ex);
        ErrorResponse errorResponse = createErrorResponse(ex, status, request);
        
        // Add provider-specific details
        if (ex.getProviderId() != null) {
            errorResponse.setDetails(List.of("Provider: " + ex.getProviderId()));
        }
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle tool execution exceptions.
     */
    @ExceptionHandler(ToolExecutionException.class)
    public ResponseEntity<ErrorResponse> handleToolExecutionException(
            ToolExecutionException ex, HttpServletRequest request) {
        
        logger.warn("Tool execution error: {}", ex.getTechnicalDetails(), ex);
        
        HttpStatus status = ex instanceof ToolExecutionException.ToolNotFoundException ? 
                HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        
        ErrorResponse errorResponse = createErrorResponse(ex, status, request);
        errorResponse.setDetails(List.of("Tool: " + ex.getToolName()));
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle memory exceptions.
     */
    @ExceptionHandler(MemoryException.class)
    public ResponseEntity<ErrorResponse> handleMemoryException(
            MemoryException ex, HttpServletRequest request) {
        
        logger.error("Memory error: {}", ex.getTechnicalDetails(), ex);
        
        HttpStatus status = ex instanceof MemoryException.CapacityExceededException ? 
                HttpStatus.INSUFFICIENT_STORAGE : HttpStatus.INTERNAL_SERVER_ERROR;
        
        ErrorResponse errorResponse = createErrorResponse(ex, status, request);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle token limit exceptions.
     */
    @ExceptionHandler(TokenLimitException.class)
    public ResponseEntity<ErrorResponse> handleTokenLimitException(
            TokenLimitException ex, HttpServletRequest request) {
        
        logger.warn("Token limit exceeded: {}", ex.getTechnicalDetails());
        
        ErrorResponse errorResponse = createErrorResponse(ex, HttpStatus.TOO_MANY_REQUESTS, request);
        errorResponse.setDetails(List.of(
                "Limit Type: " + ex.getLimitType(),
                "Usage: " + ex.getCurrentUsage() + "/" + ex.getLimit()
        ));
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * Handle configuration exceptions.
     */
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleConfigurationException(
            ConfigurationException ex, HttpServletRequest request) {
        
        logger.error("Configuration error: {}", ex.getTechnicalDetails(), ex);
        
        ErrorResponse errorResponse = createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handle security exceptions.
     */
    @ExceptionHandler(com.agent.domain.exception.SecurityException.class)
    public ResponseEntity<ErrorResponse> handleAgentSecurityException(
            com.agent.domain.exception.SecurityException ex, HttpServletRequest request) {
        
        logger.warn("Security error: {}", ex.getTechnicalDetails());
        
        HttpStatus status = ex instanceof com.agent.domain.exception.SecurityException.AuthenticationException ? 
                HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        
        ErrorResponse errorResponse = createErrorResponse(ex, status, request);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        logger.debug("Validation error: {}", ex.getTechnicalDetails());
        
        ErrorResponse errorResponse = createErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
        
        if (ex.getValidationErrors() != null) {
            errorResponse.setDetails(ex.getValidationErrors());
        } else if (ex.getFieldName() != null) {
            errorResponse.setDetails(List.of("Field: " + ex.getFieldName()));
        }
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle business logic exceptions.
     */
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicException(
            BusinessLogicException ex, HttpServletRequest request) {
        
        logger.warn("Business logic error: {}", ex.getTechnicalDetails());
        
        HttpStatus status = ex instanceof BusinessLogicException.ConversationNotFoundException ? 
                HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        
        ErrorResponse errorResponse = createErrorResponse(ex, status, request);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle external service exceptions.
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex, HttpServletRequest request) {
        
        logger.error("External service error: {}", ex.getTechnicalDetails(), ex);
        
        HttpStatus status = ex.getHttpStatusCode() > 0 ? 
                HttpStatus.valueOf(ex.getHttpStatusCode()) : HttpStatus.BAD_GATEWAY;
        
        ErrorResponse errorResponse = createErrorResponse(ex, status, request);
        errorResponse.setDetails(List.of("Service: " + ex.getServiceName()));
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle system exceptions.
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ErrorResponse> handleSystemException(
            SystemException ex, HttpServletRequest request) {
        
        logger.error("System error: {}", ex.getTechnicalDetails(), ex);
        
        HttpStatus status = ex instanceof SystemException.ResourceExhaustedException ? 
                HttpStatus.INSUFFICIENT_STORAGE : HttpStatus.INTERNAL_SERVER_ERROR;
        
        ErrorResponse errorResponse = createErrorResponse(ex, status, request);
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    // ========== Spring Framework Exception Handlers ==========
    
    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        logger.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                "Internal Server Error",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Create standardized error response from AgentException.
     */
    private ErrorResponse createErrorResponse(AgentException ex, HttpStatus status, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCategory().getDisplayName(),
                ex.getUserMessage(),
                status.value(),
                request.getRequestURI()
        );
        
        // Set additional error information
        errorResponse.setErrorCode(ex.getErrorCode());
        errorResponse.setCategory(ex.getCategory().name());
        errorResponse.setRetryable(ex.isRetryable());
        
        return errorResponse;
    }
    
    /**
     * Determine appropriate HTTP status for LLM provider exceptions.
     */
    private HttpStatus determineHttpStatus(LLMProviderException ex) {
        if (ex instanceof LLMProviderException.RateLimitExceededException) {
            return HttpStatus.TOO_MANY_REQUESTS;
        } else if (ex instanceof LLMProviderException.AuthenticationException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof LLMProviderException.ServiceUnavailableException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex.getHttpStatusCode() > 0) {
            return HttpStatus.valueOf(ex.getHttpStatusCode());
        } else {
            return HttpStatus.BAD_GATEWAY;
        }
    }
}