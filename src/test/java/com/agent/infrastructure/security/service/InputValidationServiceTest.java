package com.agent.infrastructure.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputValidationServiceTest {

    private InputValidationService inputValidationService;

    @BeforeEach
    void setUp() {
        inputValidationService = new InputValidationService();
    }

    @Test
    void shouldValidateCorrectEmailFormat() {
        assertTrue(inputValidationService.isValidEmail("user@example.com"));
        assertTrue(inputValidationService.isValidEmail("test.user+tag@domain.co.uk"));
        
        assertFalse(inputValidationService.isValidEmail("invalid-email"));
        assertFalse(inputValidationService.isValidEmail("@domain.com"));
        assertFalse(inputValidationService.isValidEmail("user@"));
        assertFalse(inputValidationService.isValidEmail(null));
    }

    @Test
    void shouldValidateUUIDFormat() {
        assertTrue(inputValidationService.isValidUUID("123e4567-e89b-12d3-a456-426614174000"));
        assertTrue(inputValidationService.isValidUUID("550e8400-e29b-41d4-a716-446655440000"));
        
        assertFalse(inputValidationService.isValidUUID("invalid-uuid"));
        assertFalse(inputValidationService.isValidUUID("123-456-789"));
        assertFalse(inputValidationService.isValidUUID(null));
    }

    @Test
    void shouldValidateAlphanumericStrings() {
        assertTrue(inputValidationService.isAlphanumeric("abc123"));
        assertTrue(inputValidationService.isAlphanumeric("ABC"));
        assertTrue(inputValidationService.isAlphanumeric("123"));
        
        assertFalse(inputValidationService.isAlphanumeric("abc-123"));
        assertFalse(inputValidationService.isAlphanumeric("abc 123"));
        assertFalse(inputValidationService.isAlphanumeric("abc@123"));
        assertFalse(inputValidationService.isAlphanumeric(null));
    }

    @Test
    void shouldDetectSqlInjectionPatterns() {
        assertTrue(inputValidationService.containsSqlInjection("1 UNION SELECT * FROM passwords"));
        assertTrue(inputValidationService.containsSqlInjection("INSERT INTO users VALUES"));
        assertTrue(inputValidationService.containsSqlInjection("DROP TABLE users"));
        assertTrue(inputValidationService.containsSqlInjection("UPDATE SET password"));
        
        assertTrue(inputValidationService.containsSqlInjection("'; DROP TABLE users; --")); // This should match DROP TABLE
        assertFalse(inputValidationService.containsSqlInjection("normal text"));
        assertFalse(inputValidationService.containsSqlInjection("user@example.com"));
        assertFalse(inputValidationService.containsSqlInjection(null));
    }

    @Test
    void shouldDetectXssPatterns() {
        assertTrue(inputValidationService.containsXss("<script>alert('xss')</script>"));
        assertTrue(inputValidationService.containsXss("javascript:alert('xss')"));
        assertTrue(inputValidationService.containsXss("<img onload='alert(1)'>"));
        assertTrue(inputValidationService.containsXss("<iframe src='evil.com'></iframe>"));
        
        assertFalse(inputValidationService.containsXss("normal text"));
        assertFalse(inputValidationService.containsXss("<p>safe html</p>"));
        assertFalse(inputValidationService.containsXss(null));
    }

    @Test
    void shouldDetectPathTraversalPatterns() {
        assertTrue(inputValidationService.containsPathTraversal("../../../etc/passwd"));
        assertTrue(inputValidationService.containsPathTraversal("..\\..\\windows\\system32"));
        assertTrue(inputValidationService.containsPathTraversal("/var/www/../../../etc/passwd"));
        
        assertFalse(inputValidationService.containsPathTraversal("normal/path/file.txt"));
        assertFalse(inputValidationService.containsPathTraversal("./current/directory"));
        assertFalse(inputValidationService.containsPathTraversal(null));
    }

    @Test
    void shouldSanitizeStrings() {
        assertEquals("", inputValidationService.sanitizeString("<script>alert('xss')</script>"));
        assertEquals("Hello World", inputValidationService.sanitizeString("Hello <b>World</b>"));
        assertEquals("alert('xss')", inputValidationService.sanitizeString("javascript:alert('xss')"));
        assertEquals("Click here", inputValidationService.sanitizeString("Click here"));
        
        assertNull(inputValidationService.sanitizeString(null));
    }

    @Test
    void shouldValidateMessageContent() {
        // Valid messages
        InputValidationService.ValidationResult result = inputValidationService.validateMessageContent("Hello, how are you?");
        assertTrue(result.isValid());

        // Empty message
        result = inputValidationService.validateMessageContent("");
        assertFalse(result.isValid());
        assertEquals("Message content cannot be empty", result.getErrorMessage());

        // Null message
        result = inputValidationService.validateMessageContent(null);
        assertFalse(result.isValid());
        assertEquals("Message content cannot be empty", result.getErrorMessage());

        // Too long message
        String longMessage = "a".repeat(10001);
        result = inputValidationService.validateMessageContent(longMessage);
        assertFalse(result.isValid());
        assertEquals("Message content too long (max 10000 characters)", result.getErrorMessage());

        // SQL injection in message
        result = inputValidationService.validateMessageContent("1 UNION SELECT * FROM users");
        assertFalse(result.isValid());
        assertEquals("Message contains potentially dangerous SQL patterns", result.getErrorMessage());

        // XSS in message
        result = inputValidationService.validateMessageContent("<script>alert('xss')</script>");
        assertFalse(result.isValid());
        assertEquals("Message contains potentially dangerous script patterns", result.getErrorMessage());
    }

    @Test
    void shouldValidateConversationId() {
        // Valid UUID
        InputValidationService.ValidationResult result = inputValidationService.validateConversationId("123e4567-e89b-12d3-a456-426614174000");
        assertTrue(result.isValid());

        // Valid alphanumeric
        result = inputValidationService.validateConversationId("conv123");
        assertTrue(result.isValid());

        // Empty ID
        result = inputValidationService.validateConversationId("");
        assertFalse(result.isValid());
        assertEquals("Conversation ID cannot be empty", result.getErrorMessage());

        // Invalid format
        result = inputValidationService.validateConversationId("conv-123-invalid!");
        assertFalse(result.isValid());
        assertEquals("Invalid conversation ID format", result.getErrorMessage());
    }

    @Test
    void shouldValidateToolParameters() {
        // Valid parameter
        InputValidationService.ValidationResult result = inputValidationService.validateToolParameter("param1", "value1");
        assertTrue(result.isValid());

        // Empty parameter name
        result = inputValidationService.validateToolParameter("", "value");
        assertFalse(result.isValid());
        assertEquals("Parameter name cannot be empty", result.getErrorMessage());

        // SQL injection in parameter
        result = inputValidationService.validateToolParameter("param", "1 UNION SELECT * FROM users");
        assertFalse(result.isValid());
        assertEquals("Parameter contains potentially dangerous SQL patterns", result.getErrorMessage());

        // XSS in parameter
        result = inputValidationService.validateToolParameter("param", "<script>alert('xss')</script>");
        assertFalse(result.isValid());
        assertEquals("Parameter contains potentially dangerous script patterns", result.getErrorMessage());

        // Path traversal in parameter
        result = inputValidationService.validateToolParameter("param", "../../../etc/passwd");
        assertFalse(result.isValid());
        assertEquals("Parameter contains path traversal patterns", result.getErrorMessage());

        // Non-string parameter (should pass)
        result = inputValidationService.validateToolParameter("param", 123);
        assertTrue(result.isValid());
    }

    @Test
    void shouldValidateApiKey() {
        // Valid API key
        InputValidationService.ValidationResult result = inputValidationService.validateApiKey("a".repeat(32));
        assertTrue(result.isValid());

        // Empty API key
        result = inputValidationService.validateApiKey("");
        assertFalse(result.isValid());
        assertEquals("API key cannot be empty", result.getErrorMessage());

        // Too short API key
        result = inputValidationService.validateApiKey("short");
        assertFalse(result.isValid());
        assertEquals("API key too short", result.getErrorMessage());

        // Too long API key
        result = inputValidationService.validateApiKey("a".repeat(129));
        assertFalse(result.isValid());
        assertEquals("API key too long", result.getErrorMessage());
    }
}