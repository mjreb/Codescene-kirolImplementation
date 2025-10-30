package com.agent.infrastructure.security;

import com.agent.infrastructure.security.service.ApiKeyService;
import com.agent.infrastructure.security.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyService apiKeyService;

    @Test
    void shouldAllowAccessToPublicEndpoints() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessToProtectedEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/conversations/123"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = {"conversation:read"})
    void shouldAllowAccessWithValidUserAndPermissions() throws Exception {
        mockMvc.perform(get("/api/conversations/123"))
                .andExpect(status().isNotFound()); // 404 because conversation doesn't exist, but auth passed
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = {"conversation:write"})
    void shouldAllowConversationCreationWithWritePermission() throws Exception {
        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agentId\":\"agent-1\",\"title\":\"Test\",\"initialMessage\":\"Hello\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = {"conversation:read"})
    void shouldDenyConversationCreationWithoutWritePermission() throws Exception {
        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agentId\":\"agent-1\",\"title\":\"Test\",\"initialMessage\":\"Hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void shouldAllowAdminAccessToAllEndpoints() throws Exception {
        mockMvc.perform(get("/api/conversations/123"))
                .andExpect(status().isNotFound()); // Auth passed, but conversation doesn't exist

        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agentId\":\"agent-1\",\"title\":\"Test\",\"initialMessage\":\"Hello\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAuthenticateWithValidApiKey() throws Exception {
        // Generate a test API key
        String apiKey = apiKeyService.generateApiKey(
                "test-user", 
                "Test Key", 
                Set.of("conversation:read", "conversation:write"), 
                null
        );

        mockMvc.perform(get("/api/conversations/123")
                .header("X-API-Key", apiKey))
                .andExpect(status().isNotFound()); // Auth passed, but conversation doesn't exist
    }

    @Test
    void shouldRejectInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/conversations/123")
                .header("X-API-Key", "invalid-api-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMaliciousInput() throws Exception {
        // SQL injection attempt
        mockMvc.perform(get("/api/conversations/'; DROP TABLE users; --"))
                .andExpect(status().isBadRequest());

        // XSS attempt
        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agentId\":\"<script>alert('xss')</script>\",\"title\":\"Test\",\"initialMessage\":\"Hello\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = {"conversation:read"})
    void shouldAddSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/conversations/valid-uuid-123"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"));
    }

    @Test
    void shouldEnforceRateLimits() throws Exception {
        String apiKey = apiKeyService.generateApiKey(
                "rate-limit-test", 
                "Rate Limit Test", 
                Set.of("conversation:read"), 
                null
        );

        // Make multiple requests rapidly
        for (int i = 0; i < 65; i++) { // Exceed the per-minute limit
            mockMvc.perform(get("/api/conversations/123")
                    .header("X-API-Key", apiKey));
        }

        // The next request should be rate limited
        mockMvc.perform(get("/api/conversations/123")
                .header("X-API-Key", apiKey))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"));
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = {"tool:execute"})
    void shouldAllowToolExecutionWithPermission() throws Exception {
        // This would test tool execution if we had a tool execution endpoint
        // For now, we'll test that the security configuration allows the request
        mockMvc.perform(post("/api/tools/calculator/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"add\",\"a\":1,\"b\":2}"))
                .andExpect(status().isNotFound()); // Endpoint doesn't exist, but auth passed
    }

    @Test
    @WithMockUser(username = "user@example.com", authorities = {"conversation:read"})
    void shouldDenyToolExecutionWithoutPermission() throws Exception {
        mockMvc.perform(post("/api/tools/calculator/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"operation\":\"add\",\"a\":1,\"b\":2}"))
                .andExpect(status().isForbidden());
    }
}