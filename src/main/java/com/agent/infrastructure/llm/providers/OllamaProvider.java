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
 * Ollama provider implementation for local LLM models.
 */
public class OllamaProvider extends BaseLLMProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaProvider.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private Set<String> availableModels;
    
    public OllamaProvider(LLMProviderConfig config) {
        super(config);
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
        this.availableModels = new HashSet<>();
        
        // Try to fetch available models on initialization
        try {
            refreshAvailableModels();
        } catch (Exception e) {
            logger.warn("Failed to fetch available models from Ollama: {}", e.getMessage());
        }
    }
    
    @Override
    protected LLMResponse doGenerateResponse(LLMRequest request) {
        try {
            String url = baseUrl + "/api/chat";
            
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = createRequestBody(request);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            logger.debug("Sending request to Ollama: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);
            
            return parseResponse(response.getBody(), request.getModel());
            
        } catch (Exception e) {
            logger.error("Ollama request failed: {}", e.getMessage());
            throw new LLMProviderException("Ollama request failed: " + e.getMessage(), e, 
                                         isRetryableException(e), getProviderId());
        }
    }
    
    @Override
    protected boolean performHealthCheck() {
        try {
            String url = baseUrl + "/api/tags";
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            boolean isHealthy = response.getStatusCode() == HttpStatus.OK;
            if (isHealthy) {
                // Refresh available models on successful health check
                refreshAvailableModels();
            }
            
            return isHealthy;
            
        } catch (Exception e) {
            logger.debug("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean supportsModel(String model) {
        if (model == null) {
            return false;
        }
        
        // If we haven't fetched models yet, try to refresh
        if (availableModels.isEmpty()) {
            try {
                refreshAvailableModels();
            } catch (Exception e) {
                logger.debug("Failed to refresh available models: {}", e.getMessage());
            }
        }
        
        return availableModels.contains(model.toLowerCase()) || 
               availableModels.stream().anyMatch(m -> m.startsWith(model.toLowerCase()));
    }
    
    @Override
    public int estimateTokenCount(LLMRequest request) {
        // Simple estimation for local models: ~3.5 characters per token
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
        
        // Add minimal overhead for local models
        return (int) (totalChars / 3.5) + 10;
    }
    
    /**
     * Refresh the list of available models from Ollama.
     */
    private void refreshAvailableModels() {
        try {
            String url = baseUrl + "/api/tags";
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode modelsArray = root.path("models");
                
                Set<String> models = new HashSet<>();
                if (modelsArray.isArray()) {
                    for (JsonNode modelNode : modelsArray) {
                        String modelName = modelNode.path("name").asText();
                        if (!modelName.isEmpty()) {
                            models.add(modelName.toLowerCase());
                        }
                    }
                }
                
                this.availableModels = models;
                logger.debug("Refreshed available Ollama models: {}", models);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to refresh available models: {}", e.getMessage());
        }
    }
    
    /**
     * Create HTTP headers for Ollama API requests.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Ollama typically doesn't require authentication for local instances
        // but we'll add the API key if provided
        if (config.getApiKey() != null && !config.getApiKey().trim().isEmpty()) {
            headers.setBearerAuth(config.getApiKey());
        }
        
        // Add custom headers if configured
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(headers::add);
        }
        
        return headers;
    }
    
    /**
     * Create request body for Ollama API.
     */
    private Map<String, Object> createRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Set model
        String model = request.getModel() != null ? request.getModel() : "llama2";
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
        
        // Ollama-specific options
        Map<String, Object> options = new HashMap<>();
        
        if (request.getTemperature() > 0) {
            options.put("temperature", request.getTemperature());
        }
        
        if (request.getMaxTokens() > 0) {
            options.put("num_predict", request.getMaxTokens());
        }
        
        // Add custom parameters as options
        if (request.getParameters() != null) {
            request.getParameters().forEach((key, value) -> {
                if (!body.containsKey(key)) {
                    options.put(key, value);
                }
            });
        }
        
        if (!options.isEmpty()) {
            body.put("options", options);
        }
        
        // Disable streaming for synchronous requests
        body.put("stream", false);
        
        return body;
    }
    
    /**
     * Convert internal message type to Ollama role.
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
     * Parse Ollama API response.
     */
    private LLMResponse parseResponse(String responseBody, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            // Extract content
            String content = root.path("message").path("content").asText();
            
            // Create basic token usage (Ollama doesn't provide detailed token counts)
            TokenUsage tokenUsage = new TokenUsage();
            
            // Estimate tokens based on content length
            int outputTokens = estimateTokensFromText(content);
            tokenUsage.setOutputTokens(outputTokens);
            
            // We don't have input token count from Ollama, so we'll estimate
            int inputTokens = estimateTokenCount(new LLMRequest()) - outputTokens;
            tokenUsage.setInputTokens(Math.max(0, inputTokens));
            tokenUsage.setTotalTokens(tokenUsage.getInputTokens() + tokenUsage.getOutputTokens());
            
            // Create response
            LLMResponse response = new LLMResponse(content, model, getProviderId());
            response.setTokenUsage(tokenUsage);
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("done", root.path("done").asBoolean());
            metadata.put("total_duration", root.path("total_duration").asLong());
            metadata.put("load_duration", root.path("load_duration").asLong());
            metadata.put("prompt_eval_count", root.path("prompt_eval_count").asInt());
            metadata.put("eval_count", root.path("eval_count").asInt());
            response.setMetadata(metadata);
            
            return response;
            
        } catch (Exception e) {
            throw new LLMProviderException("Failed to parse Ollama response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Estimate token count from text content.
     */
    private int estimateTokensFromText(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) (text.length() / 3.5);
    }
    
    @Override
    protected boolean isRetryableException(Exception e) {
        if (super.isRetryableException(e)) {
            return true;
        }
        
        // Ollama specific retryable conditions
        String message = e.getMessage();
        if (message != null) {
            return message.contains("connection refused") ||
                   message.contains("model not found") ||
                   message.contains("loading model") ||
                   message.contains("timeout");
        }
        
        return false;
    }
    
    /**
     * Get the set of available models.
     */
    public Set<String> getAvailableModels() {
        return new HashSet<>(availableModels);
    }
}