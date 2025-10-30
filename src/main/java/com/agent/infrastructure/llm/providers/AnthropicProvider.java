package com.agent.infrastructure.llm.providers;

import com.agent.domain.model.*;
import com.agent.infrastructure.llm.BaseLLMProvider;
import com.agent.infrastructure.llm.LLMProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Anthropic provider implementation for Claude models.
 */
public class AnthropicProvider extends BaseLLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(AnthropicProvider.class);
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final Set<String> SUPPORTED_MODELS = Set.of(
        "claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307",
        "claude-2.1", "claude-2.0", "claude-instant-1.2"
    );
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    
    public AnthropicProvider(LLMProviderConfig config) {
        super(config);
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
    }
    
    @Override
    protected LLMResponse doGenerateResponse(LLMRequest request) {
        try {
            String url = baseUrl + "/messages";
            
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = createRequestBody(request);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            logger.debug("Sending request to Anthropic: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            return parseResponse(response.getBody(), request.getModel());
            
        } catch (Exception e) {
            logger.error("Anthropic request failed: {}", e.getMessage());
            throw new LLMProviderException("Anthropic request failed: " + e.getMessage(), e, 
                                         isRetryableException(e), getProviderId());
        }
    }
    
    @Override
    protected boolean performHealthCheck() {
        try {
            // Anthropic doesn't have a dedicated health endpoint, so we'll make a minimal request
            String url = baseUrl + "/messages";
            HttpHeaders headers = createHeaders();
            
            Map<String, Object> testBody = new HashMap<>();
            testBody.put("model", "claude-3-haiku-20240307");
            testBody.put("max_tokens", 1);
            testBody.put("messages", List.of(
                Map.of("role", "user", "content", "Hi")
            ));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(testBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            logger.debug("Anthropic health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean supportsModel(String model) {
        return model != null && SUPPORTED_MODELS.contains(model.toLowerCase());
    }
    
    @Override
    public int estimateTokenCount(LLMRequest request) {
        // Simple estimation: ~4 characters per token for English text
        int totalChars = 0;
        
        if (request.getPrompt() != null) {
            totalChars += request.getPrompt().length();
        }
        
        if (request.getMessages() != null) {
            for (Message message : request.getMessages()) {
                if (message.getContent() != null) {
                    totalChars += message.getContent().length();
                }
            }
        }
        
        // Add some overhead for message formatting
        return (totalChars / 4) + 30;
    }
    
    /**
     * Create HTTP headers for Anthropic API requests.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", config.getApiKey());
        headers.set("anthropic-version", "2023-06-01");
        
        // Add custom headers if configured
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(headers::add);
        }
        
        return headers;
    }
    
    /**
     * Create request body for Anthropic API.
     */
    private Map<String, Object> createRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Set model
        String model = request.getModel() != null ? request.getModel() : "claude-3-haiku-20240307";
        body.put("model", model);
        
        // Convert messages
        List<Map<String, String>> messages = new ArrayList<>();
        String systemMessage = null;
        
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            for (Message message : request.getMessages()) {
                if (message.getType() == Message.MessageType.SYSTEM) {
                    // Anthropic handles system messages separately
                    systemMessage = message.getContent();
                } else {
                    Map<String, String> msgMap = new HashMap<>();
                    msgMap.put("role", convertMessageType(message.getType()));
                    msgMap.put("content", message.getContent());
                    messages.add(msgMap);
                }
            }
        } else if (request.getPrompt() != null) {
            // Convert single prompt to user message
            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", "user");
            msgMap.put("content", request.getPrompt());
            messages.add(msgMap);
        }
        
        body.put("messages", messages);
        
        // Set system message if present
        if (systemMessage != null) {
            body.put("system", systemMessage);
        }
        
        // Set parameters
        int maxTokens = request.getMaxTokens() > 0 ? request.getMaxTokens() : 1024;
        body.put("max_tokens", maxTokens);
        
        if (request.getTemperature() > 0) {
            body.put("temperature", request.getTemperature());
        }
        
        // Add custom parameters
        if (request.getParameters() != null) {
            request.getParameters().forEach((key, value) -> {
                if (!body.containsKey(key)) {
                    body.put(key, value);
                }
            });
        }
        
        return body;
    }
    
    /**
     * Convert internal message type to Anthropic role.
     */
    private String convertMessageType(Message.MessageType type) {
        switch (type) {
            case USER:
                return "user";
            case ASSISTANT:
                return "assistant";
            case SYSTEM:
                return "user"; // System messages are handled separately in Anthropic
            default:
                return "user";
        }
    }
    
    /**
     * Parse Anthropic API response.
     */
    private LLMResponse parseResponse(String responseBody, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            // Extract content
            String content = "";
            JsonNode contentArray = root.path("content");
            if (contentArray.isArray() && contentArray.size() > 0) {
                content = contentArray.get(0).path("text").asText();
            }
            
            // Extract token usage
            TokenUsage tokenUsage = null;
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                tokenUsage = new TokenUsage();
                tokenUsage.setInputTokens(usageNode.path("input_tokens").asInt());
                tokenUsage.setOutputTokens(usageNode.path("output_tokens").asInt());
                tokenUsage.setTotalTokens(tokenUsage.getInputTokens() + tokenUsage.getOutputTokens());
            }
            
            // Create response
            LLMResponse response = new LLMResponse(content, model, getProviderId());
            response.setTokenUsage(tokenUsage);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("stop_reason", root.path("stop_reason").asText());
            metadata.put("response_id", root.path("id").asText());
            metadata.put("model", root.path("model").asText());
            response.setMetadata(metadata);
            
            return response;
            
        } catch (Exception e) {
            throw new LLMProviderException("Failed to parse Anthropic response: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected boolean isRetryableException(Exception e) {
        if (super.isRetryableException(e)) {
            return true;
        }
        
        // Anthropic specific retryable conditions
        String message = e.getMessage();
        if (message != null) {
            return message.contains("rate_limit") ||
                   message.contains("overloaded") ||
                   message.contains("timeout") ||
                   message.contains("502") ||
                   message.contains("503") ||
                   message.contains("504");
        }
        
        return false;
    }
}