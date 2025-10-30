package com.agent.infrastructure.tools.impl;

import com.agent.domain.model.ParameterDefinition;
import com.agent.domain.model.ToolDefinition;
import com.agent.domain.model.ToolResult;
import com.agent.infrastructure.tools.BaseTool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculator tool for performing mathematical operations.
 */
@Component
public class CalculatorTool extends BaseTool {
    
    public CalculatorTool() {
        super(createDefinition());
    }
    
    private static ToolDefinition createDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("calculator");
        definition.setDescription("Performs mathematical calculations including basic arithmetic operations");
        definition.setCategory("utility");
        definition.setAsync(false);
        
        Map<String, ParameterDefinition> parameters = new HashMap<>();
        
        // Expression parameter
        ParameterDefinition expression = new ParameterDefinition();
        expression.setName("expression");
        expression.setType("string");
        expression.setDescription("Mathematical expression to evaluate (e.g., '2 + 3 * 4')");
        expression.setRequired(true);
        parameters.put("expression", expression);
        
        // Precision parameter (optional)
        ParameterDefinition precision = new ParameterDefinition();
        precision.setName("precision");
        precision.setType("integer");
        precision.setDescription("Number of decimal places for the result (default: 10)");
        precision.setRequired(false);
        precision.setDefaultValue(10);
        precision.setMinValue(0);
        precision.setMaxValue(20);
        parameters.put("precision", precision);
        
        definition.setParameters(parameters);
        return definition;
    }
    
    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters) {
        String expression = getRequiredParameter(parameters, "expression", String.class);
        Integer precision = getParameter(parameters, "precision", Integer.class);
        
        if (precision == null) {
            precision = 10;
        }
        
        try {
            BigDecimal result = evaluateExpression(expression.trim());
            
            // Round to specified precision
            result = result.setScale(precision, RoundingMode.HALF_UP);
            
            // Convert to appropriate number type for cleaner output
            if (result.scale() == 0 && result.abs().compareTo(new BigDecimal(Long.MAX_VALUE)) <= 0) {
                return createSuccessResult(result.longValue());
            } else {
                return createSuccessResult(result.doubleValue());
            }
            
        } catch (Exception e) {
            return createErrorResult("Invalid mathematical expression: " + e.getMessage());
        }
    }
    
    /**
     * Evaluates a mathematical expression using a simple recursive descent parser.
     * Supports +, -, *, /, parentheses, and decimal numbers.
     */
    private BigDecimal evaluateExpression(String expression) {
        return new ExpressionParser(expression).parse();
    }
    
    /**
     * Simple recursive descent parser for mathematical expressions.
     */
    private static class ExpressionParser {
        private final String expression;
        private int position = 0;
        
        public ExpressionParser(String expression) {
            this.expression = expression.replaceAll("\\s+", ""); // Remove whitespace
        }
        
        public BigDecimal parse() {
            BigDecimal result = parseExpression();
            if (position < expression.length()) {
                throw new IllegalArgumentException("Unexpected character at position " + position);
            }
            return result;
        }
        
        private BigDecimal parseExpression() {
            BigDecimal result = parseTerm();
            
            while (position < expression.length()) {
                char op = expression.charAt(position);
                if (op == '+' || op == '-') {
                    position++;
                    BigDecimal right = parseTerm();
                    if (op == '+') {
                        result = result.add(right);
                    } else {
                        result = result.subtract(right);
                    }
                } else {
                    break;
                }
            }
            
            return result;
        }
        
        private BigDecimal parseTerm() {
            BigDecimal result = parseFactor();
            
            while (position < expression.length()) {
                char op = expression.charAt(position);
                if (op == '*' || op == '/') {
                    position++;
                    BigDecimal right = parseFactor();
                    if (op == '*') {
                        result = result.multiply(right);
                    } else {
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            throw new ArithmeticException("Division by zero");
                        }
                        try {
                            // Try exact division first
                            result = result.divide(right);
                        } catch (ArithmeticException e) {
                            // If not exact, use precision
                            result = result.divide(right, 20, RoundingMode.HALF_UP);
                        }
                    }
                } else {
                    break;
                }
            }
            
            return result;
        }
        
        private BigDecimal parseFactor() {
            if (position >= expression.length()) {
                throw new IllegalArgumentException("Unexpected end of expression");
            }
            
            char ch = expression.charAt(position);
            
            // Handle negative numbers
            if (ch == '-') {
                position++;
                return parseFactor().negate();
            }
            
            // Handle positive numbers (optional + sign)
            if (ch == '+') {
                position++;
                return parseFactor();
            }
            
            // Handle parentheses
            if (ch == '(') {
                position++;
                BigDecimal result = parseExpression();
                if (position >= expression.length() || expression.charAt(position) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                position++;
                return result;
            }
            
            // Parse number
            return parseNumber();
        }
        
        private BigDecimal parseNumber() {
            int start = position;
            
            while (position < expression.length()) {
                char ch = expression.charAt(position);
                if (Character.isDigit(ch) || ch == '.') {
                    position++;
                } else {
                    break;
                }
            }
            
            if (start == position) {
                throw new IllegalArgumentException("Expected number at position " + position);
            }
            
            String numberStr = expression.substring(start, position);
            try {
                return new BigDecimal(numberStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number: " + numberStr);
            }
        }
    }
}