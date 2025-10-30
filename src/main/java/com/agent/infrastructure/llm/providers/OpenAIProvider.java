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
 * OpenAI provider implementation for GPT models.
 */
public class OpenAIProvider extends BaseLLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIProvider.class);
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final Set<String> SUPPORTED_MODELS = Set.of(
        "gpt-4", "gpt-4-turbo", "gpt-4-turbo-preview", "gpt-4-0125-preview",
        "gpt-3.5-turbo", "gpt-3.5-turbo-0125", "gpt-3.5-turbo-1106"
    );
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    
    public OpenAIProvider(LLMProviderConfig config) {
        super(config);
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
    }
    
    @Override
    protected LLMResponse doGenerateResponse(LLMRequest request) {
        try {
            String url = baseUrl + "/chat/completions";
            
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = createRequestBody(request);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            logger.debug("Sending request to OpenAI: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            return parseResponse(response.getBody(), request.getModel());
            
        } catch (Exception e) {
            logger.error("OpenAI request failed: {}", e.getMessage());
            throw new LLMProviderException("OpenAI request failed: " + e.getMessage(), e, 
                                         isRetryableException(e), getProviderId());
        }
    }
    
    @Override
    protected boolean performHealthCheck() {
        try {
            String url = baseUrl + "/models";
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            logger.debug("OpenAI health check failed: {}", e.getMessage());
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
        
        // Add some overhead for message formatting and system tokens
        return (totalChars / 4) + 50;
    }
    
    /**
     * Create HTTP headers for OpenAI API requests.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());
        
        // Add custom headers if configured
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(headers::add);
        }
        
        return headers;
    }
    
    /**
     * Create request body for OpenAI API.
     */
    private Map<String, Object> createRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Set model
        String model = request.getModel() != null ? request.getModel() : "gpt-3.5-turbo";
        body.put("model", model);
        
        // Convert messages
        List<Map<String, String>> messages = new ArrayList<>();
        
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            for (Message message : request.getMessages()) {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("role", convertMessageType(message.getType()));
                msgMap.put("content", message.getContent());
                messages.add(msgMap);
            }
        } else if (request.getPrompt() != null) {
            // Convert single prompt to user message
            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", "user");
            msgMap.put("content", request.getPrompt());
            messages.add(msgMap);
        }
        
        body.put("messages", messages);
        
        // Set parameters
        if (request.getMaxTokens() > 0) {
            body.put("max_tokens", request.getMaxTokens());
        }
        
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
     * Convert internal message type to OpenAI role.
     */
    private String convertMessageType(Message.MessageType type) {
        switch (type) {
            case USER:
                return "user";
            case ASSISTANT:
                return "assistant";
            case SYSTEM:
                return "system";
            default:
                return "user";
        }
    }
    
    /**
     * Parse OpenAI API response.
     */
    private LLMResponse parseResponse(String responseBody, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            // Extract content
            String content = root.path("choices")
                                .get(0)
                                .path("message")
                                .path("content")
                                .asText();
            
            // Extract token usage
            TokenUsage tokenUsage = null;
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                tokenUsage = new TokenUsage();
                tokenUsage.setInputTokens(usageNode.path("prompt_tokens").asInt());
                tokenUsage.setOutputTokens(usageNode.path("completion_tokens").asInt());
                tokenUsage.setTotalTokens(usageNode.path("total_tokens").asInt());
            }
            
            // Create response
            LLMResponse response = new LLMResponse(content, model, getProviderId());
            response.setTokenUsage(tokenUsage);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("finish_reason", root.path("choices").get(0).path("finish_reason").asText());
            metadata.put("response_id", root.path("id").asText());
            response.setMetadata(metadata);
            
            return response;
            
        } catch (Exception e) {
            throw new LLMProviderException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected boolean isRetryableException(Exception e) {
        if (super.isRetryableException(e)) {
            return true;
        }
        
        // OpenAI specific retryable conditions
        String message = e.getMessage();
        if (message != null) {
            return message.contains("rate limit") ||
                   message.contains("timeout") ||
                   message.contains("502") ||
                   message.contains("503") ||
                   message.contains("504");
        }
        
        return false;
    }
}