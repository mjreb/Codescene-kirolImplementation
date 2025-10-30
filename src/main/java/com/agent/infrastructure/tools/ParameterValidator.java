package com.agent.infrastructure.tools;

import com.agent.domain.model.ParameterDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Utility class for validating and converting tool parameters according to their definitions.
 */
public class ParameterValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ParameterValidator.class);
    
    /**
     * Validates and converts a parameter value according to its definition.
     * 
     * @param paramDef The parameter definition
     * @param value The value to validate and convert
     * @return The converted value
     * @throws IllegalArgumentException if validation fails
     */
    public Object validateAndConvert(ParameterDefinition paramDef, Object value) {
        if (value == null) {
            return null;
        }
        
        String type = paramDef.getType();
        if (type == null) {
            return value; // No type specified, return as-is
        }
        
        try {
            Object convertedValue = convertToType(value, type);
            validateConstraints(paramDef, convertedValue);
            return convertedValue;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Parameter '%s' validation failed: %s", paramDef.getName(), e.getMessage()), e);
        }
    }
    
    /**
     * Converts a value to the specified type.
     */
    private Object convertToType(Object value, String type) {
        if (value == null) {
            return null;
        }
        
        return switch (type.toLowerCase()) {
            case "string" -> convertToString(value);
            case "integer", "int" -> convertToInteger(value);
            case "long" -> convertToLong(value);
            case "double", "number" -> convertToDouble(value);
            case "boolean", "bool" -> convertToBoolean(value);
            case "object" -> value; // Return as-is for objects
            default -> {
                logger.warn("Unknown parameter type '{}', returning value as-is", type);
                yield value;
            }
        };
    }
    
    private String convertToString(Object value) {
        return value.toString();
    }
    
    private Integer convertToInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + value + "' to integer");
            }
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to integer");
    }
    
    private Long convertToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + value + "' to long");
            }
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to long");
    }
    
    private Double convertToDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + value + "' to double");
            }
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to double");
    }
    
    private Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String str = ((String) value).toLowerCase().trim();
            return switch (str) {
                case "true", "yes", "1", "on" -> true;
                case "false", "no", "0", "off" -> false;
                default -> throw new IllegalArgumentException("Cannot convert '" + value + "' to boolean");
            };
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0.0;
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getSimpleName() + " to boolean");
    }
    
    /**
     * Validates constraints defined in the parameter definition.
     */
    private void validateConstraints(ParameterDefinition paramDef, Object value) {
        if (value == null) {
            return;
        }
        
        // Pattern validation for strings
        if (paramDef.getPattern() != null && value instanceof String) {
            Pattern pattern = Pattern.compile(paramDef.getPattern());
            if (!pattern.matcher((String) value).matches()) {
                throw new IllegalArgumentException(
                    String.format("Value '%s' does not match pattern '%s'", value, paramDef.getPattern()));
            }
        }
        
        // Range validation for numbers
        if (value instanceof Number) {
            validateNumberRange(paramDef, (Number) value);
        }
        
        // String length validation
        if (value instanceof String) {
            validateStringLength(paramDef, (String) value);
        }
    }
    
    private void validateNumberRange(ParameterDefinition paramDef, Number value) {
        BigDecimal numValue = new BigDecimal(value.toString());
        
        if (paramDef.getMinValue() != null) {
            BigDecimal minValue = new BigDecimal(paramDef.getMinValue().toString());
            if (numValue.compareTo(minValue) < 0) {
                throw new IllegalArgumentException(
                    String.format("Value %s is less than minimum %s", value, paramDef.getMinValue()));
            }
        }
        
        if (paramDef.getMaxValue() != null) {
            BigDecimal maxValue = new BigDecimal(paramDef.getMaxValue().toString());
            if (numValue.compareTo(maxValue) > 0) {
                throw new IllegalArgumentException(
                    String.format("Value %s is greater than maximum %s", value, paramDef.getMaxValue()));
            }
        }
    }
    
    private void validateStringLength(ParameterDefinition paramDef, String value) {
        if (paramDef.getMinValue() != null) {
            int minLength = ((Number) paramDef.getMinValue()).intValue();
            if (value.length() < minLength) {
                throw new IllegalArgumentException(
                    String.format("String length %d is less than minimum %d", value.length(), minLength));
            }
        }
        
        if (paramDef.getMaxValue() != null) {
            int maxLength = ((Number) paramDef.getMaxValue()).intValue();
            if (value.length() > maxLength) {
                throw new IllegalArgumentException(
                    String.format("String length %d is greater than maximum %d", value.length(), maxLength));
            }
        }
    }
}