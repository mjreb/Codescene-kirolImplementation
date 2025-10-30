package com.agent.infrastructure.react;

import com.agent.domain.model.ToolCall;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses LLM responses in the ReAct pattern format.
 */
@Component
public class ReActResponseParser {
    
    private static final Logger logger = LoggerFactory.getLogger(ReActResponseParser.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Patterns for parsing different response types
    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "Action:\\s*([^\\n]+)\\s*\\n?Parameters:\\s*(.+?)(?=\\n\\n|\\n[A-Z]|$)", 
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile(
        "Final Answer:\\s*(.+)", 
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern THOUGHT_PATTERN = Pattern.compile(
        "Thought:\\s*(.+?)(?=\\n\\n|\\nAction:|\\nFinal Answer:|$)", 
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Parse a thinking phase response to extract thought and potential action.
     */
    public ReActThought parseThinkingResponse(String response) {
        logger.debug("Parsing thinking response: {}", response);
        
        ReActThought thought = new ReActThought();
        
        // Extract thought
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(response);
        if (thoughtMatcher.find()) {
            thought.setThought(thoughtMatcher.group(1).trim());
        } else {
            // If no explicit thought pattern, use the whole response as thought
            thought.setThought(response.trim());
        }
        
        // Check for final answer
        Matcher finalAnswerMatcher = FINAL_ANSWER_PATTERN.matcher(response);
        if (finalAnswerMatcher.find()) {
            thought.setFinalAnswer(finalAnswerMatcher.group(1).trim());
            return thought;
        }
        
        // Check for action
        Matcher actionMatcher = ACTION_PATTERN.matcher(response);
        if (actionMatcher.find()) {
            String toolName = actionMatcher.group(1).trim();
            String parametersStr = actionMatcher.group(2).trim();
            
            try {
                Map<String, Object> parameters = parseParameters(parametersStr);
                ToolCall toolCall = new ToolCall(toolName, parameters);
                toolCall.setCallId(generateCallId());
                thought.setAction(toolCall);
            } catch (Exception e) {
                logger.warn("Failed to parse action parameters for tool '{}': {}", toolName, parametersStr, e);
                // Create action with empty parameters as fallback
                ToolCall toolCall = new ToolCall(toolName, new HashMap<>());
                toolCall.setCallId(generateCallId());
                thought.setAction(toolCall);
            }
        }
        
        return thought;
    }
    
    /**
     * Parse an observing phase response to determine next steps.
     */
    public ReActObservation parseObservingResponse(String response) {
        logger.debug("Parsing observing response: {}", response);
        
        ReActObservation observation = new ReActObservation();
        
        // Check for final answer
        Matcher finalAnswerMatcher = FINAL_ANSWER_PATTERN.matcher(response);
        if (finalAnswerMatcher.find()) {
            observation.setComplete(true);
            observation.setFinalAnswer(finalAnswerMatcher.group(1).trim());
            return observation;
        }
        
        // Check for continued thought
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(response);
        if (thoughtMatcher.find()) {
            observation.setComplete(false);
            observation.setNextThought(thoughtMatcher.group(1).trim());
            return observation;
        }
        
        // If no clear pattern, assume we need to continue thinking
        observation.setComplete(false);
        observation.setNextThought(response.trim());
        
        return observation;
    }
    
    /**
     * Parse parameter string into a map with enhanced error handling.
     */
    private Map<String, Object> parseParameters(String parametersStr) {
        if (parametersStr == null || parametersStr.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        parametersStr = parametersStr.trim();
        
        try {
            // Try to parse as JSON first
            if (parametersStr.startsWith("{") && parametersStr.endsWith("}")) {
                return objectMapper.readValue(parametersStr, new TypeReference<Map<String, Object>>() {});
            }
            
            // Try to parse as simple key-value pairs
            return parseKeyValueParameters(parametersStr);
            
        } catch (Exception e) {
            logger.warn("Failed to parse parameters: {}", parametersStr, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Parse key-value parameter format with type conversion.
     */
    private Map<String, Object> parseKeyValueParameters(String parametersStr) {
        Map<String, Object> parameters = new HashMap<>();
        
        // Handle different separators and formats
        String[] pairs = parametersStr.split(",");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length >= 2) {
                String key = keyValue[0].trim().replaceAll("[\"\']", "");
                String value = String.join(":", Arrays.copyOfRange(keyValue, 1, keyValue.length))
                                   .trim().replaceAll("[\"\']", "");
                
                // Convert value to appropriate type
                Object convertedValue = convertParameterValue(value);
                parameters.put(key, convertedValue);
            }
        }
        
        return parameters;
    }
    
    /**
     * Convert parameter value to appropriate type.
     */
    private Object convertParameterValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        
        value = value.trim();
        
        // Try boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        
        // Try integer
        try {
            if (!value.contains(".")) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException ignored) {}
        
        // Try double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}
        
        // Return as string
        return value;
    }
    
    /**
     * Generate a unique call ID for tool calls.
     */
    private String generateCallId() {
        return "call_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Represents a parsed thought from the thinking phase.
     */
    public static class ReActThought {
        private String thought;
        private ToolCall action;
        private String finalAnswer;
        
        public String getThought() { return thought; }
        public void setThought(String thought) { this.thought = thought; }
        
        public ToolCall getAction() { return action; }
        public void setAction(ToolCall action) { this.action = action; }
        
        public String getFinalAnswer() { return finalAnswer; }
        public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }
        
        public boolean hasAction() { return action != null; }
        public boolean isFinalAnswer() { return finalAnswer != null; }
    }
    
    /**
     * Represents a parsed observation from the observing phase.
     */
    public static class ReActObservation {
        private boolean complete;
        private String finalAnswer;
        private String nextThought;
        
        public boolean isComplete() { return complete; }
        public void setComplete(boolean complete) { this.complete = complete; }
        
        public String getFinalAnswer() { return finalAnswer; }
        public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }
        
        public String getNextThought() { return nextThought; }
        public void setNextThought(String nextThought) { this.nextThought = nextThought; }
    }
}