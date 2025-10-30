package com.agent.infrastructure.llm;

import com.agent.domain.model.*;
import com.agent.infrastructure.llm.providers.OpenAIProvider;
import com.agent.infrastructure.llm.providers.AnthropicProvider;
import com.agent.infrastructure.llm.providers.OllamaProvider;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LLM providers using WireMock to simulate external APIs.
 */
class LLMProviderIntegrationTest {
    
    private WireMockServer wireMockServer;
    private LLMProviderManagerImpl providerManager;
    
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
        
        providerManager = new LLMProviderManagerImpl();
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    void testOpenAIProviderIntegration() {
        // Setup mock OpenAI API responses
        setupOpenAIMocks();
        
        // Create OpenAI provider with mock server URL
        LLMProviderConfig config = new LLMProviderConfig("openai", "OpenAI", "test-key");
        config.setBaseUrl("http://localhost:8089/v1");
        
        OpenAIProvider provider = new OpenAIProvider(config);
        providerManager.registerProvider(provider);
        
        // Test health check
        ProviderHealth health = providerManager.checkProviderHealth("openai");
        assertEquals(ProviderHealth.HealthStatus.HEALTHY, health.getStatus());
        
        // Test generate response
        LLMRequest request = new LLMRequest("Hello", "gpt-3.5-turbo");
        LLMResponse response = providerManager.generateResponse(request, "openai");
        
        assertNotNull(response);
        assertEquals("Hello! How can I help you today?", response.getContent());
        assertEquals("gpt-3.5-turbo", response.getModel());
        assertEquals("openai", response.getProviderId());
        
        // Verify token usage
        TokenUsage tokenUsage = response.getTokenUsage();
        assertNotNull(tokenUsage);
        assertEquals(9, tokenUsage.getInputTokens());
        assertEquals(12, tokenUsage.getOutputTokens());
        assertEquals(21, tokenUsage.getTotalTokens());
    }
    
    @Test
    void testAnthropicProviderIntegration() {
        // Setup mock Anthropic API responses
        setupAnthropicMocks();
        
        // Create Anthropic provider with mock server URL
        LLMProviderConfig config = new LLMProviderConfig("anthropic", "Anthropic", "test-key");
        config.setBaseUrl("http://localhost:8089/v1");
        
        AnthropicProvider provider = new AnthropicProvider(config);
        providerManager.registerProvider(provider);
        
        // Test generate response
        LLMRequest request = new LLMRequest("Hello", "claude-3-haiku-20240307");
        LLMResponse response = providerManager.generateResponse(request, "anthropic");
        
        assertNotNull(response);
        assertEquals("Hello! How can I help you today?", response.getContent());
        assertEquals("claude-3-haiku-20240307", response.getModel());
        assertEquals("anthropic", response.getProviderId());
        
        // Verify token usage
        TokenUsage tokenUsage = response.getTokenUsage();
        assertNotNull(tokenUsage);
        assertEquals(10, tokenUsage.getInputTokens());
        assertEquals(12, tokenUsage.getOutputTokens());
    }
    
    @Test
    void testOllamaProviderIntegration() {
        // Setup mock Ollama API responses
        setupOllamaMocks();
        
        // Create Ollama provider with mock server URL
        LLMProviderConfig config = new LLMProviderConfig("ollama", "Ollama", "");
        config.setBaseUrl("http://localhost:8089");
        
        OllamaProvider provider = new OllamaProvider(config);
        providerManager.registerProvider(provider);
        
        // Test health check (which fetches available models)
        ProviderHealth health = providerManager.checkProviderHealth("ollama");
        assertEquals(ProviderHealth.HealthStatus.HEALTHY, health.getStatus());
        
        // Test generate response
        LLMRequest request = new LLMRequest("Hello", "llama2");
        LLMResponse response = providerManager.generateResponse(request, "ollama");
        
        assertNotNull(response);
        assertEquals("Hello! How can I help you today?", response.getContent());
        assertEquals("llama2", response.getModel());
        assertEquals("ollama", response.getProviderId());
    }
    
    @Test
    void testProviderFailoverMechanism() {
        // Setup mocks where first provider fails, second succeeds
        setupFailoverMocks();
        
        // Create two providers
        LLMProviderConfig config1 = new LLMProviderConfig("provider1", "Provider1", "key1");
        config1.setBaseUrl("http://localhost:8089/v1");
        
        LLMProviderConfig config2 = new LLMProviderConfig("provider2", "Provider2", "key2");
        config2.setBaseUrl("http://localhost:8089/v2");
        
        OpenAIProvider provider1 = new OpenAIProvider(config1);
        OpenAIProvider provider2 = new OpenAIProvider(config2);
        
        providerManager.registerProvider(provider1);
        providerManager.registerProvider(provider2);
        
        // Test failover (don't specify provider ID to trigger automatic selection)
        LLMRequest request = new LLMRequest("Hello", "gpt-3.5-turbo");
        LLMResponse response = providerManager.generateResponse(request, null);
        
        assertNotNull(response);
        assertEquals("Backup response", response.getContent());
        assertEquals("provider2", response.getProviderId());
    }
    
    @Test
    void testRetryMechanism() {
        // Setup retry configuration
        LLMProviderConfig config = new LLMProviderConfig("openai", "OpenAI", "test-key");
        config.setBaseUrl("http://localhost:8089/v1");
        
        LLMProviderConfig.RetryConfig retryConfig = new LLMProviderConfig.RetryConfig();
        retryConfig.setMaxRetries(2);
        retryConfig.setInitialDelayMs(100);
        config.setRetryConfig(retryConfig);
        
        // Setup mock to fail twice then succeed
        setupRetryMocks();
        
        OpenAIProvider provider = new OpenAIProvider(config);
        providerManager.registerProvider(provider);
        
        LLMRequest request = new LLMRequest("Hello", "gpt-3.5-turbo");
        LLMResponse response = providerManager.generateResponse(request, "openai");
        
        assertNotNull(response);
        assertEquals("Success after retry", response.getContent());
    }
    
    @Test
    void testTokenCountingAccuracy() {
        setupOpenAIMocks();
        
        LLMProviderConfig config = new LLMProviderConfig("openai", "OpenAI", "test-key");
        config.setBaseUrl("http://localhost:8089/v1");
        
        OpenAIProvider provider = new OpenAIProvider(config);
        
        // Test token estimation
        LLMRequest request = new LLMRequest("This is a test message for token counting", "gpt-3.5-turbo");
        int estimatedTokens = provider.estimateTokenCount(request);
        
        assertTrue(estimatedTokens > 0);
        assertTrue(estimatedTokens < 50); // Should be reasonable for this text
        
        // Test with actual response
        LLMResponse response = provider.generateResponse(request);
        TokenUsage actualUsage = response.getTokenUsage();
        
        // Estimation should be in the right ballpark (within 50% of actual)
        assertTrue(Math.abs(estimatedTokens - actualUsage.getInputTokens()) < actualUsage.getInputTokens() * 0.5);
    }
    
    private void setupOpenAIMocks() {
        // Mock models endpoint for health check
        stubFor(get(urlEqualTo("/v1/models"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"data\": []}")));
        
        // Mock chat completions endpoint
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "id": "chatcmpl-123",
                        "object": "chat.completion",
                        "created": 1677652288,
                        "choices": [{
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "Hello! How can I help you today?"
                            },
                            "finish_reason": "stop"
                        }],
                        "usage": {
                            "prompt_tokens": 9,
                            "completion_tokens": 12,
                            "total_tokens": 21
                        }
                    }
                    """)));
    }
    
    private void setupAnthropicMocks() {
        // Mock messages endpoint
        stubFor(post(urlEqualTo("/v1/messages"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "id": "msg_123",
                        "type": "message",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": "Hello! How can I help you today?"
                            }
                        ],
                        "model": "claude-3-haiku-20240307",
                        "stop_reason": "end_turn",
                        "usage": {
                            "input_tokens": 10,
                            "output_tokens": 12
                        }
                    }
                    """)));
    }
    
    private void setupOllamaMocks() {
        // Mock tags endpoint for health check and model discovery
        stubFor(get(urlEqualTo("/api/tags"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "models": [
                            {"name": "llama2:latest"},
                            {"name": "codellama:7b"}
                        ]
                    }
                    """)));
        
        // Mock chat endpoint
        stubFor(post(urlEqualTo("/api/chat"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "model": "llama2",
                        "created_at": "2023-08-04T19:22:45.499127Z",
                        "message": {
                            "role": "assistant",
                            "content": "Hello! How can I help you today?"
                        },
                        "done": true,
                        "total_duration": 5191566416,
                        "load_duration": 2154458,
                        "prompt_eval_count": 26,
                        "eval_count": 12
                    }
                    """)));
    }
    
    private void setupFailoverMocks() {
        // First provider fails
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")));
        
        // Second provider succeeds
        stubFor(post(urlEqualTo("/v2/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "id": "chatcmpl-backup",
                        "choices": [{
                            "message": {
                                "role": "assistant",
                                "content": "Backup response"
                            },
                            "finish_reason": "stop"
                        }],
                        "usage": {"prompt_tokens": 5, "completion_tokens": 5, "total_tokens": 10}
                    }
                    """)));
    }
    
    private void setupRetryMocks() {
        // Fail twice, then succeed
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .inScenario("Retry Test")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("First Retry"));
        
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .inScenario("Retry Test")
            .whenScenarioStateIs("First Retry")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("Second Retry"));
        
        stubFor(post(urlEqualTo("/v1/chat/completions"))
            .inScenario("Retry Test")
            .whenScenarioStateIs("Second Retry")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "id": "chatcmpl-retry-success",
                        "choices": [{
                            "message": {
                                "role": "assistant",
                                "content": "Success after retry"
                            },
                            "finish_reason": "stop"
                        }],
                        "usage": {"prompt_tokens": 5, "completion_tokens": 5, "total_tokens": 10}
                    }
                    """)));
    }
}