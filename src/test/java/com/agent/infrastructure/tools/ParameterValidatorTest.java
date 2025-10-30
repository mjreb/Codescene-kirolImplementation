package com.agent.infrastructure.tools;

import com.agent.domain.model.ParameterDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParameterValidatorTest {
    
    private ParameterValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new ParameterValidator();
    }
    
    @Test
    void testValidateStringParameter() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("string");
        
        Object result = validator.validateAndConvert(paramDef, "hello");
        assertEquals("hello", result);
    }
    
    @Test
    void testValidateIntegerParameter() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("integer");
        
        Object result = validator.validateAndConvert(paramDef, "123");
        assertEquals(123, result);
        
        result = validator.validateAndConvert(paramDef, 456);
        assertEquals(456, result);
    }
    
    @Test
    void testValidateDoubleParameter() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("double");
        
        Object result = validator.validateAndConvert(paramDef, "123.45");
        assertEquals(123.45, result);
        
        result = validator.validateAndConvert(paramDef, 67.89);
        assertEquals(67.89, result);
    }
    
    @Test
    void testValidateBooleanParameter() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("boolean");
        
        Object result = validator.validateAndConvert(paramDef, "true");
        assertEquals(true, result);
        
        result = validator.validateAndConvert(paramDef, "false");
        assertEquals(false, result);
        
        result = validator.validateAndConvert(paramDef, "yes");
        assertEquals(true, result);
        
        result = validator.validateAndConvert(paramDef, "no");
        assertEquals(false, result);
        
        result = validator.validateAndConvert(paramDef, "1");
        assertEquals(true, result);
        
        result = validator.validateAndConvert(paramDef, "0");
        assertEquals(false, result);
        
        result = validator.validateAndConvert(paramDef, 1);
        assertEquals(true, result);
        
        result = validator.validateAndConvert(paramDef, 0);
        assertEquals(false, result);
    }
    
    @Test
    void testValidateWithPattern() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("email");
        paramDef.setType("string");
        paramDef.setPattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        
        // Valid email
        Object result = validator.validateAndConvert(paramDef, "test@example.com");
        assertEquals("test@example.com", result);
        
        // Invalid email
        assertThrows(IllegalArgumentException.class, () -> 
            validator.validateAndConvert(paramDef, "invalid-email"));
    }
    
    @Test
    void testValidateNumberRange() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("age");
        paramDef.setType("integer");
        paramDef.setMinValue(0);
        paramDef.setMaxValue(150);
        
        // Valid age
        Object result = validator.validateAndConvert(paramDef, 25);
        assertEquals(25, result);
        
        // Too young
        assertThrows(IllegalArgumentException.class, () -> 
            validator.validateAndConvert(paramDef, -1));
        
        // Too old
        assertThrows(IllegalArgumentException.class, () -> 
            validator.validateAndConvert(paramDef, 200));
    }
    
    @Test
    void testValidateStringLength() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("username");
        paramDef.setType("string");
        paramDef.setMinValue(3); // Minimum length
        paramDef.setMaxValue(20); // Maximum length
        
        // Valid username
        Object result = validator.validateAndConvert(paramDef, "john_doe");
        assertEquals("john_doe", result);
        
        // Too short
        assertThrows(IllegalArgumentException.class, () -> 
            validator.validateAndConvert(paramDef, "ab"));
        
        // Too long
        assertThrows(IllegalArgumentException.class, () -> 
            validator.validateAndConvert(paramDef, "this_username_is_way_too_long_for_validation"));
    }
    
    @Test
    void testValidateInvalidIntegerConversion() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("integer");
        
        assertThrows(IllegalArgumentException.class, () -> 
            validator.validateAndConvert(paramDef, "not_a_number"));
    }
    
    @Test
    void testValidateInvalidBooleanConversion() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("boolean");
        
        assertThrows(IllegalArgumentException.class, () -> 
            validator.validateAndConvert(paramDef, "maybe"));
    }
    
    @Test
    void testValidateNullValue() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("string");
        
        Object result = validator.validateAndConvert(paramDef, null);
        assertNull(result);
    }
    
    @Test
    void testValidateUnknownType() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("unknown_type");
        
        // Should return value as-is for unknown types
        Object result = validator.validateAndConvert(paramDef, "some_value");
        assertEquals("some_value", result);
    }
    
    @Test
    void testValidateNoType() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        // No type specified
        
        // Should return value as-is when no type is specified
        Object result = validator.validateAndConvert(paramDef, "some_value");
        assertEquals("some_value", result);
    }
    
    @Test
    void testConvertNumberToString() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("string");
        
        Object result = validator.validateAndConvert(paramDef, 123);
        assertEquals("123", result);
        
        result = validator.validateAndConvert(paramDef, 45.67);
        assertEquals("45.67", result);
    }
    
    @Test
    void testConvertStringToNumber() {
        ParameterDefinition paramDef = new ParameterDefinition();
        paramDef.setName("test");
        paramDef.setType("long");
        
        Object result = validator.validateAndConvert(paramDef, "9876543210");
        assertEquals(9876543210L, result);
    }
}