package com.agent.infrastructure.tools.impl;

import com.agent.domain.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorToolTest {
    
    private CalculatorTool calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new CalculatorTool();
    }
    
    @Test
    void testBasicArithmetic() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "2 + 3");
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(5.0, result.getResult());
    }
    
    @Test
    void testMultiplication() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "4 * 5");
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(20.0, result.getResult());
    }
    
    @Test
    void testDivision() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "10 / 2");
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(5.0, result.getResult());
    }
    
    @Test
    void testDecimalResult() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "10 / 3");
        params.put("precision", 2);
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(3.33, result.getResult());
    }
    
    @Test
    void testComplexExpression() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "2 + 3 * 4 - 1");
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(13.0, result.getResult()); // 2 + 12 - 1 = 13
    }
    
    @Test
    void testParentheses() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "(2 + 3) * 4");
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(20.0, result.getResult()); // 5 * 4 = 20
    }
    
    @Test
    void testNegativeNumbers() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "-5 + 3");
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(-2.0, result.getResult());
    }
    
    @Test
    void testDecimalNumbers() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "3.14 * 2");
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(6.28, result.getResult());
    }
    
    @Test
    void testDivisionByZero() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "5 / 0");
        
        ToolResult result = calculator.execute(params);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Division by zero"));
    }
    
    @Test
    void testInvalidExpression() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "2 + * 3"); // Invalid: operator after operator
        
        ToolResult result = calculator.execute(params);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    void testMissingExpression() {
        Map<String, Object> params = new HashMap<>();
        // No expression parameter
        
        ToolResult result = calculator.execute(params);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    void testEmptyExpression() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "");
        
        ToolResult result = calculator.execute(params);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    void testWhitespaceHandling() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "  2   +   3  ");
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(5.0, result.getResult());
    }
    
    @Test
    void testPrecisionParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "22 / 7");
        params.put("precision", 5);
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        assertEquals(3.14286, result.getResult());
    }
    
    @Test
    void testDefaultPrecision() {
        Map<String, Object> params = new HashMap<>();
        params.put("expression", "1 / 3");
        // No precision specified, should use default
        
        ToolResult result = calculator.execute(params);
        assertTrue(result.isSuccess());
        // Should have default precision (10 decimal places)
        assertTrue(result.getResult().toString().contains("0.3333333333"));
    }
    
    @Test
    void testToolDefinition() {
        assertNotNull(calculator.getDefinition());
        assertEquals("calculator", calculator.getDefinition().getName());
        assertNotNull(calculator.getDefinition().getDescription());
        assertFalse(calculator.getDefinition().isAsync());
        
        // Should have expression and precision parameters
        assertTrue(calculator.getDefinition().getParameters().containsKey("expression"));
        assertTrue(calculator.getDefinition().getParameters().containsKey("precision"));
        
        // Expression should be required
        assertTrue(calculator.getDefinition().getParameters().get("expression").isRequired());
        
        // Precision should be optional with default value
        assertFalse(calculator.getDefinition().getParameters().get("precision").isRequired());
        assertEquals(10, calculator.getDefinition().getParameters().get("precision").getDefaultValue());
    }
}