package com.agent.domain.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception for input validation and request processing errors.
 */
public class ValidationException extends AgentException {
    
    private final String fieldName;
    private final Object rejectedValue;
    private final List<String> validationErrors;
    
    public ValidationException(String message) {
        super("VALIDATION_ERROR", ErrorCategory.VALIDATION, message, false);
        this.fieldName = null;
        this.rejectedValue = null;
        this.validationErrors = null;
    }
    
    public ValidationException(String fieldName, Object rejectedValue, String message) {
        super("VALIDATION_ERROR", ErrorCategory.VALIDATION, message, false);
        this.fieldName = fieldName;
        this.rejectedValue = rejectedValue;
        this.validationErrors = null;
    }
    
    public ValidationException(List<String> validationErrors) {
        super("VALIDATION_ERROR", ErrorCategory.VALIDATION, "Multiple validation errors", false);
        this.fieldName = null;
        this.rejectedValue = null;
        this.validationErrors = validationErrors;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    @Override
    public String getUserMessage() {
        if (validationErrors != null && !validationErrors.isEmpty()) {
            return "Request validation failed: " + String.join(", ", validationErrors);
        } else if (fieldName != null) {
            return String.format("Invalid value for field '%s': %s", fieldName, getMessage());
        } else {
            return "Request validation failed: " + getMessage();
        }
    }
    
    /**
     * Required field missing exception.
     */
    public static class RequiredFieldException extends ValidationException {
        public RequiredFieldException(String fieldName) {
            super(fieldName, null, "Required field is missing");
        }
        
        @Override
        public String getUserMessage() {
            return String.format("Required field '%s' is missing.", getFieldName());
        }
    }
    
    /**
     * Invalid field format exception.
     */
    public static class InvalidFormatException extends ValidationException {
        private final String expectedFormat;
        
        public InvalidFormatException(String fieldName, Object rejectedValue, String expectedFormat) {
            super(fieldName, rejectedValue, 
                    String.format("Invalid format for field '%s': expected %s", fieldName, expectedFormat));
            this.expectedFormat = expectedFormat;
        }
        
        public String getExpectedFormat() {
            return expectedFormat;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("Invalid format for field '%s': expected %s", getFieldName(), expectedFormat);
        }
    }
    
    /**
     * Field value out of range exception.
     */
    public static class OutOfRangeException extends ValidationException {
        private final Object minValue;
        private final Object maxValue;
        
        public OutOfRangeException(String fieldName, Object rejectedValue, Object minValue, Object maxValue) {
            super(fieldName, rejectedValue, 
                    String.format("Value out of range for field '%s': must be between %s and %s", 
                            fieldName, minValue, maxValue));
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
        
        public Object getMinValue() {
            return minValue;
        }
        
        public Object getMaxValue() {
            return maxValue;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("Value for field '%s' must be between %s and %s", 
                    getFieldName(), minValue, maxValue);
        }
    }
}