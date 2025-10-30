package com.agent.presentation.exception;

import com.agent.presentation.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private MethodArgumentNotValidException validationException;
    
    @Mock
    private BindingResult bindingResult;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/test/endpoint");
    }
    
    @Test
    void handleValidationErrors_ReturnsValidationErrorResponse() {
        // Given
        FieldError fieldError1 = new FieldError("object", "field1", "Field1 is required");
        FieldError fieldError2 = new FieldError("object", "field2", "Field2 must be valid");
        
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationErrors(validationException, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("Validation Failed", errorResponse.getError());
        assertEquals("Request validation failed", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
        assertEquals("/test/endpoint", errorResponse.getPath());
        assertNotNull(errorResponse.getDetails());
        assertEquals(2, errorResponse.getDetails().size());
        assertTrue(errorResponse.getDetails().contains("Field1 is required"));
        assertTrue(errorResponse.getDetails().contains("Field2 must be valid"));
    }
    
    @Test
    void handleIllegalArgument_ReturnsBadRequestResponse() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(exception, request);
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Invalid argument provided", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
        assertEquals("/test/endpoint", errorResponse.getPath());
    }
    
    @Test
    void handleSecurityException_ReturnsForbiddenResponse() {
        // Given
        SecurityException exception = new SecurityException("Access denied to resource");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJavaSecurityException(exception, request);
        
        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("Access Denied", errorResponse.getError());
        assertEquals("Access denied to resource", errorResponse.getMessage());
        assertEquals(403, errorResponse.getStatus());
        assertEquals("/test/endpoint", errorResponse.getPath());
    }
    
    @Test
    void handleIllegalState_ReturnsConflictResponse() {
        // Given
        IllegalStateException exception = new IllegalStateException("Invalid state for operation");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalState(exception, request);
        
        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("Invalid State", errorResponse.getError());
        assertEquals("Invalid state for operation", errorResponse.getMessage());
        assertEquals(409, errorResponse.getStatus());
        assertEquals("/test/endpoint", errorResponse.getPath());
    }
    
    @Test
    void handleRuntimeException_ReturnsInternalServerErrorResponse() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected runtime error");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("An unexpected error occurred", errorResponse.getMessage());
        assertEquals(500, errorResponse.getStatus());
        assertEquals("/test/endpoint", errorResponse.getPath());
    }
    
    @Test
    void handleGenericException_ReturnsInternalServerErrorResponse() {
        // Given
        Exception exception = new Exception("Generic exception");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, request);
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ErrorResponse errorResponse = response.getBody();
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("An unexpected error occurred", errorResponse.getMessage());
        assertEquals(500, errorResponse.getStatus());
        assertEquals("/test/endpoint", errorResponse.getPath());
    }
}