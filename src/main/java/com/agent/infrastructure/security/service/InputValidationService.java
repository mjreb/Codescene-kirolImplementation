package com.agent.infrastructure.security.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for comprehensive input validation and sanitization
 */
@Service
public class InputValidationService {

    // Common patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    // Dangerous patterns to detect
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union\\s+select|insert\\s+into|update\\s+set|delete\\s+from|drop\\s+table|create\\s+table|alter\\s+table|exec\\s*\\(|execute\\s*\\().*"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(<script|javascript:|vbscript:|onload=|onerror=|<iframe|<object|<embed).*"
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        ".*(\\.\\.[\\\\/]|[\\\\/]\\.\\.[\\\\/]|\\.\\.\\\\|\\.\\./).*"
    );

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate UUID format
     */
    public boolean isValidUUID(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid).matches();
    }

    /**
     * Validate alphanumeric string
     */
    public boolean isAlphanumeric(String input) {
        return input != null && ALPHANUMERIC_PATTERN.matcher(input).matches();
    }

    /**
     * Check for SQL injection patterns
     */
    public boolean containsSqlInjection(String input) {
        return input != null && SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Check for XSS patterns
     */
    public boolean containsXss(String input) {
        return input != null && XSS_PATTERN.matcher(input).find();
    }

    /**
     * Check for path traversal patterns
     */
    public boolean containsPathTraversal(String input) {
        return input != null && PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Sanitize string by removing dangerous characters
     */
    public String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        return input
                .replaceAll("(?i)<script[^>]*>.*?</script>", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("(?i)javascript:", "")
                .replaceAll("(?i)vbscript:", "")
                .replaceAll("(?i)onload\\s*=", "")
                .replaceAll("(?i)onerror\\s*=", "")
                .trim();
    }

    /**
     * Validate conversation message content
     */
    public ValidationResult validateMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return ValidationResult.invalid("Message content cannot be empty");
        }
        
        if (content.length() > 10000) {
            return ValidationResult.invalid("Message content too long (max 10000 characters)");
        }
        
        if (containsXss(content)) {
            return ValidationResult.invalid("Message contains potentially dangerous script patterns");
        }
        
        if (containsSqlInjection(content)) {
            return ValidationResult.invalid("Message contains potentially dangerous SQL patterns");
        }
        
        return ValidationResult.valid();
    }

    /**
     * Validate conversation ID
     */
    public ValidationResult validateConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return ValidationResult.invalid("Conversation ID cannot be empty");
        }
        
        if (!isValidUUID(conversationId) && !isAlphanumeric(conversationId)) {
            return ValidationResult.invalid("Invalid conversation ID format");
        }
        
        return ValidationResult.valid();
    }

    /**
     * Validate user input for tool parameters
     */
    public ValidationResult validateToolParameter(String paramName, Object value) {
        if (paramName == null || paramName.trim().isEmpty()) {
            return ValidationResult.invalid("Parameter name cannot be empty");
        }
        
        if (value instanceof String) {
            String stringValue = (String) value;
            
            if (containsXss(stringValue)) {
                return ValidationResult.invalid("Parameter contains potentially dangerous script patterns");
            }
            
            if (containsSqlInjection(stringValue)) {
                return ValidationResult.invalid("Parameter contains potentially dangerous SQL patterns");
            }
            
            if (containsPathTraversal(stringValue)) {
                return ValidationResult.invalid("Parameter contains path traversal patterns");
            }
        }
        
        return ValidationResult.valid();
    }

    /**
     * Validate API key format
     */
    public ValidationResult validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ValidationResult.invalid("API key cannot be empty");
        }
        
        if (apiKey.length() < 32) {
            return ValidationResult.invalid("API key too short");
        }
        
        if (apiKey.length() > 128) {
            return ValidationResult.invalid("API key too long");
        }
        
        return ValidationResult.valid();
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}