package com.agent.infrastructure.fallback;

import com.agent.domain.model.AgentResponse;
import com.agent.domain.model.LLMResponse;
import com.agent.domain.model.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that provides fallback responses when primary services are unavailable.
 * Implements graceful degradation patterns.
 */
@Service
public class FallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(FallbackService.class);
    
    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private final SystemStatusService systemStatusService;
    
    public FallbackService(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }
    
    // ========== LLM Provider Fallbacks ==========
    
    /**
     * Provide fallback LLM response when primary providers are unavailable.
     */
    public LLMResponse getFallbackLLMResponse(String prompt, String providerId) {
        logger.warn("Providing fallback LLM response for provider: {}", providerId);
        
        // Try to get cached response first
        CachedResponse cached = getCachedResponse(prompt);
        if (cached != null && cached.isValid()) {
            logger.info("Using cached response for fallback");
            return createLLMResponseFromCache(cached);
        }
        
        // Generate generic fallback response
        String fallbackMessage = generateFallbackMessage();
        
        LLMResponse response = new LLMResponse();
        response.setContent(fallbackMessage);
        response.setProviderId("fallback");
        response.setTimestamp(Instant.now());
        
        return response;
    }
    
    /**
     * Provide fallback agent response when system is degraded.
     */
    public AgentResponse getFallbackAgentResponse(String conversationId, String userMessage) {
        logger.warn("Providing fallback agent response for conversation: {}", conversationId);
        
        String fallbackMessage = generateAgentFallbackMessage();
        
        AgentResponse response = new AgentResponse();
        response.setContent(fallbackMessage);
        response.setType(AgentResponse.ResponseType.ERROR);
        response.setTimestamp(Instant.now());
        
        return response;
    }
    
    // ========== Tool Execution Fallbacks ==========
    
    /**
     * Provide fallback tool result when tool execution fails.
     */
    public ToolResult getFallbackToolResult(String toolName, Map<String, Object> parameters) {
        logger.warn("Providing fallback tool result for tool: {}", toolName);
        
        ToolResult result = new ToolResult();
        result.setToolName(toolName);
        result.setSuccess(false);
        result.setErrorMessage("Tool temporarily unavailable. Please try again later.");
        result.setExecutionTime(Instant.now());
        
        // Provide tool-specific fallback messages
        switch (toolName.toLowerCase()) {
            case "calculator":
                result.setResult("Unable to perform calculation at this time.");
                break;
            case "websearch":
                result.setResult("Search service is temporarily unavailable.");
                break;
            case "filesystem":
                result.setResult("File system access is temporarily restricted.");
                break;
            default:
                result.setResult("Tool service is temporarily unavailable.");
        }
        
        return result;
    }
    
    // ========== Response Caching ==========
    
    /**
     * Cache successful response for future fallback use.
     */
    public void cacheResponse(String key, String content, String type) {
        logger.debug("Caching response for key: {}", key);
        
        CachedResponse cached = new CachedResponse(content, type, Instant.now());
        responseCache.put(generateCacheKey(key), cached);
        
        // Clean up old cache entries
        cleanupCache();
    }
    
    /**
     * Get cached response if available and valid.
     */
    private CachedResponse getCachedResponse(String key) {
        return responseCache.get(generateCacheKey(key));
    }
    
    /**
     * Generate cache key from input.
     */
    private String generateCacheKey(String input) {
        return String.valueOf(input.hashCode());
    }
    
    /**
     * Clean up expired cache entries.
     */
    private void cleanupCache() {
        Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour TTL
        responseCache.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(cutoff));
    }
    
    // ========== System Status Communication ==========
    
    /**
     * Get system status message for users.
     */
    public String getSystemStatusMessage() {
        SystemStatus status = systemStatusService.getCurrentStatus();
        
        if (status.isFullyOperational()) {
            return null; // No message needed
        }
        
        StringBuilder message = new StringBuilder();
        message.append("System Status: ");
        
        if (status.isLLMProvidersDown()) {
            message.append("AI services are experiencing issues. ");
        }
        
        if (status.isToolsDown()) {
            message.append("Some tools may be unavailable. ");
        }
        
        if (status.isMemoryIssues()) {
            message.append("Conversation history may be limited. ");
        }
        
        message.append("We're working to restore full functionality.");
        
        return message.toString();
    }
    
    /**
     * Check if system is in degraded mode.
     */
    public boolean isSystemDegraded() {
        return !systemStatusService.getCurrentStatus().isFullyOperational();
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Generate generic fallback message.
     */
    private String generateFallbackMessage() {
        List<String> fallbackMessages = List.of(
                "I'm experiencing some technical difficulties right now. Please try again in a moment.",
                "My AI services are temporarily unavailable. I'll be back to full functionality shortly.",
                "I'm currently running in limited mode. Some features may not be available.",
                "There's a temporary issue with my AI capabilities. Please bear with me while this is resolved."
        );
        
        int index = (int) (System.currentTimeMillis() % fallbackMessages.size());
        return fallbackMessages.get(index);
    }
    
    /**
     * Generate agent-specific fallback message.
     */
    private String generateAgentFallbackMessage() {
        String statusMessage = getSystemStatusMessage();
        String baseMessage = "I'm currently experiencing some technical issues and may not be able to provide my usual level of assistance.";
        
        if (statusMessage != null) {
            return baseMessage + " " + statusMessage;
        }
        
        return baseMessage + " Please try again in a few minutes.";
    }
    
    /**
     * Create LLM response from cached content.
     */
    private LLMResponse createLLMResponseFromCache(CachedResponse cached) {
        LLMResponse response = new LLMResponse();
        response.setContent(cached.getContent());
        response.setProviderId("cached");
        response.setTimestamp(Instant.now());
        
        return response;
    }
    
    /**
     * Estimate token count for text.
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        // Rough estimation: ~4 characters per token
        return text.length() / 4;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Cached response data.
     */
    private static class CachedResponse {
        private final String content;
        private final String type;
        private final Instant timestamp;
        
        public CachedResponse(String content, String type, Instant timestamp) {
            this.content = content;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        public String getContent() { return content; }
        public String getType() { return type; }
        public Instant getTimestamp() { return timestamp; }
        
        public boolean isValid() {
            // Cache is valid for 1 hour
            return timestamp.isAfter(Instant.now().minusSeconds(3600));
        }
    }
}