package com.agent.deployment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for API documentation generation and accuracy.
 * Verifies that OpenAPI documentation is properly generated and matches expectations.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class ApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Test
    void testOpenApiDocsGeneration() throws Exception {
        // Test that OpenAPI docs are generated and accessible
        MvcResult result = mockMvc.perform(get("/api/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertFalse(content.isEmpty(), "OpenAPI docs should not be empty");

        // Parse and validate JSON structure
        JsonNode apiDocs = jsonMapper.readTree(content);
        
        // Verify required OpenAPI fields
        assertTrue(apiDocs.has("openapi"), "Should have openapi version");
        assertTrue(apiDocs.has("info"), "Should have info section");
        assertTrue(apiDocs.has("paths"), "Should have paths section");
        assertTrue(apiDocs.has("components"), "Should have components section");

        // Verify OpenAPI version
        assertEquals("3.0.1", apiDocs.get("openapi").asText(), 
                "Should use OpenAPI 3.0.1");

        // Verify info section
        JsonNode info = apiDocs.get("info");
        assertTrue(info.has("title"), "Should have title");
        assertTrue(info.has("version"), "Should have version");
        assertTrue(info.has("description"), "Should have description");
        
        assertEquals("Agent Application API", info.get("title").asText());
    }

    @Test
    void testSwaggerUiAccessible() throws Exception {
        // Test that Swagger UI is accessible
        mockMvc.perform(get("/api/swagger-ui.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    @Test
    void testApiDocsContainExpectedEndpoints() throws Exception {
        // Test that all expected endpoints are documented
        MvcResult result = mockMvc.perform(get("/api/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = jsonMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = apiDocs.get("paths");

        // Verify authentication endpoints
        assertTrue(paths.has("/auth/login"), "Should document login endpoint");
        assertTrue(paths.has("/auth/register"), "Should document register endpoint");
        assertTrue(paths.has("/auth/refresh"), "Should document refresh endpoint");

        // Verify conversation endpoints
        assertTrue(paths.has("/conversations"), "Should document conversations endpoint");
        assertTrue(paths.has("/conversations/{conversationId}"), 
                "Should document conversation by ID endpoint");
        assertTrue(paths.has("/conversations/{conversationId}/messages"), 
                "Should document send message endpoint");

        // Verify monitoring endpoints
        assertTrue(paths.has("/actuator/health"), "Should document health endpoint");
    }

    @Test
    void testApiDocsContainSecuritySchemes() throws Exception {
        // Test that security schemes are properly documented
        MvcResult result = mockMvc.perform(get("/api/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = jsonMapper.readTree(result.getResponse().getContentAsString());
        JsonNode components = apiDocs.get("components");
        JsonNode securitySchemes = components.get("securitySchemes");

        assertNotNull(securitySchemes, "Should have security schemes");
        assertTrue(securitySchemes.has("bearerAuth"), "Should have bearer auth scheme");
        assertTrue(securitySchemes.has("apiKeyAuth"), "Should have API key auth scheme");

        // Verify bearer auth configuration
        JsonNode bearerAuth = securitySchemes.get("bearerAuth");
        assertEquals("http", bearerAuth.get("type").asText());
        assertEquals("bearer", bearerAuth.get("scheme").asText());
        assertEquals("JWT", bearerAuth.get("bearerFormat").asText());

        // Verify API key auth configuration
        JsonNode apiKeyAuth = securitySchemes.get("apiKeyAuth");
        assertEquals("apiKey", apiKeyAuth.get("type").asText());
        assertEquals("header", apiKeyAuth.get("in").asText());
        assertEquals("X-API-Key", apiKeyAuth.get("name").asText());
    }

    @Test
    void testApiDocsContainSchemas() throws Exception {
        // Test that request/response schemas are documented
        MvcResult result = mockMvc.perform(get("/api/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = jsonMapper.readTree(result.getResponse().getContentAsString());
        JsonNode components = apiDocs.get("components");
        JsonNode schemas = components.get("schemas");

        assertNotNull(schemas, "Should have schemas");

        // Verify key schemas exist
        assertTrue(schemas.has("AuthenticationRequest"), 
                "Should have AuthenticationRequest schema");
        assertTrue(schemas.has("AuthenticationResponse"), 
                "Should have AuthenticationResponse schema");
        assertTrue(schemas.has("CreateConversationRequest"), 
                "Should have CreateConversationRequest schema");
        assertTrue(schemas.has("ConversationResponse"), 
                "Should have ConversationResponse schema");
        assertTrue(schemas.has("ErrorResponse"), 
                "Should have ErrorResponse schema");
    }

    @Test
    void testApiDocsContainExamples() throws Exception {
        // Test that endpoints contain examples
        MvcResult result = mockMvc.perform(get("/api/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = jsonMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = apiDocs.get("paths");

        // Check login endpoint has examples
        JsonNode loginPost = paths.get("/auth/login").get("post");
        JsonNode requestBody = loginPost.get("requestBody");
        JsonNode content = requestBody.get("content").get("application/json");
        
        assertTrue(content.has("examples") || content.has("example"), 
                "Login endpoint should have request examples");

        // Check responses have examples
        JsonNode responses = loginPost.get("responses");
        JsonNode successResponse = responses.get("200");
        JsonNode responseContent = successResponse.get("content").get("application/json");
        
        assertTrue(responseContent.has("examples") || responseContent.has("example"), 
                "Login endpoint should have response examples");
    }

    @Test
    void testStaticOpenApiFileExists() throws IOException {
        // Test that the static OpenAPI file exists and is valid
        Path openApiPath = Paths.get("docs/openapi.yaml");
        assertTrue(Files.exists(openApiPath), "Static OpenAPI file should exist");

        String yamlContent = Files.readString(openApiPath);
        assertFalse(yamlContent.isEmpty(), "OpenAPI file should not be empty");

        // Parse YAML and validate structure
        JsonNode yamlDoc = yamlMapper.readTree(yamlContent);
        
        assertTrue(yamlDoc.has("openapi"), "YAML should have openapi version");
        assertTrue(yamlDoc.has("info"), "YAML should have info section");
        assertTrue(yamlDoc.has("paths"), "YAML should have paths section");
        
        assertEquals("3.0.3", yamlDoc.get("openapi").asText(), 
                "YAML should use OpenAPI 3.0.3");
    }

    @Test
    void testPostmanCollectionExists() throws IOException {
        // Test that Postman collection exists and is valid JSON
        Path postmanPath = Paths.get("docs/Agent-Application-API.postman_collection.json");
        assertTrue(Files.exists(postmanPath), "Postman collection should exist");

        String jsonContent = Files.readString(postmanPath);
        assertFalse(jsonContent.isEmpty(), "Postman collection should not be empty");

        // Parse and validate JSON structure
        JsonNode collection = jsonMapper.readTree(jsonContent);
        
        assertTrue(collection.has("info"), "Collection should have info");
        assertTrue(collection.has("item"), "Collection should have items");
        assertTrue(collection.has("variable"), "Collection should have variables");

        // Verify collection info
        JsonNode info = collection.get("info");
        assertEquals("Agent Application API", info.get("name").asText());
        assertTrue(info.has("description"), "Collection should have description");

        // Verify collection has expected folders
        JsonNode items = collection.get("item");
        assertTrue(items.isArray(), "Items should be an array");
        assertTrue(items.size() > 0, "Collection should have items");

        // Check for expected folders
        boolean hasAuth = false, hasConversations = false, hasMonitoring = false;
        for (JsonNode item : items) {
            String name = item.get("name").asText();
            if ("Authentication".equals(name)) hasAuth = true;
            if ("Conversations".equals(name)) hasConversations = true;
            if ("Monitoring".equals(name)) hasMonitoring = true;
        }

        assertTrue(hasAuth, "Collection should have Authentication folder");
        assertTrue(hasConversations, "Collection should have Conversations folder");
        assertTrue(hasMonitoring, "Collection should have Monitoring folder");
    }

    @Test
    void testDocumentationReadmeExists() throws IOException {
        // Test that documentation README exists
        Path readmePath = Paths.get("docs/README.md");
        assertTrue(Files.exists(readmePath), "Documentation README should exist");

        String content = Files.readString(readmePath);
        assertFalse(content.isEmpty(), "README should not be empty");

        // Verify README contains expected sections
        assertTrue(content.contains("# Agent Application API Documentation"), 
                "README should have main title");
        assertTrue(content.contains("## Quick Start"), 
                "README should have Quick Start section");
        assertTrue(content.contains("## Authentication"), 
                "README should have Authentication section");
        assertTrue(content.contains("## Example Usage"), 
                "README should have Example Usage section");
    }

    @Test
    void testApiDocsMatchStaticFile() throws Exception {
        // Test that generated docs are consistent with static file
        // This is a basic consistency check - in practice you might want more detailed comparison
        
        MvcResult result = mockMvc.perform(get("/api/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode generatedDocs = jsonMapper.readTree(result.getResponse().getContentAsString());
        
        Path openApiPath = Paths.get("docs/openapi.yaml");
        if (Files.exists(openApiPath)) {
            String yamlContent = Files.readString(openApiPath);
            JsonNode staticDocs = yamlMapper.readTree(yamlContent);
            
            // Compare basic structure
            assertEquals(staticDocs.get("info").get("title").asText(), 
                    generatedDocs.get("info").get("title").asText(),
                    "Title should match between generated and static docs");
        }
    }

    @Test
    void testApiDocsValidation() throws Exception {
        // Test that generated OpenAPI docs are valid
        MvcResult result = mockMvc.perform(get("/api/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiDocs = jsonMapper.readTree(result.getResponse().getContentAsString());
        
        // Basic validation checks
        validateOpenApiStructure(apiDocs);
        validatePaths(apiDocs.get("paths"));
        validateComponents(apiDocs.get("components"));
    }

    private void validateOpenApiStructure(JsonNode apiDocs) {
        // Validate required OpenAPI fields
        assertNotNull(apiDocs.get("openapi"), "OpenAPI version is required");
        assertNotNull(apiDocs.get("info"), "Info object is required");
        assertNotNull(apiDocs.get("paths"), "Paths object is required");
        
        // Validate info object
        JsonNode info = apiDocs.get("info");
        assertNotNull(info.get("title"), "Title is required in info");
        assertNotNull(info.get("version"), "Version is required in info");
    }

    private void validatePaths(JsonNode paths) {
        // Validate that paths contain proper HTTP methods and responses
        paths.fields().forEachRemaining(entry -> {
            String path = entry.getKey();
            JsonNode pathItem = entry.getValue();
            
            // Each path should have at least one HTTP method
            boolean hasMethod = pathItem.has("get") || pathItem.has("post") || 
                              pathItem.has("put") || pathItem.has("delete") || 
                              pathItem.has("patch");
            assertTrue(hasMethod, "Path " + path + " should have at least one HTTP method");
            
            // Validate operations
            pathItem.fields().forEachRemaining(methodEntry -> {
                String method = methodEntry.getKey();
                if (isHttpMethod(method)) {
                    JsonNode operation = methodEntry.getValue();
                    assertNotNull(operation.get("responses"), 
                            "Operation " + method + " " + path + " should have responses");
                }
            });
        });
    }

    private void validateComponents(JsonNode components) {
        if (components != null && components.has("schemas")) {
            JsonNode schemas = components.get("schemas");
            
            // Validate that schemas have proper structure
            schemas.fields().forEachRemaining(entry -> {
                String schemaName = entry.getKey();
                JsonNode schema = entry.getValue();
                
                assertTrue(schema.has("type") || schema.has("$ref") || schema.has("allOf") || 
                          schema.has("oneOf") || schema.has("anyOf"),
                        "Schema " + schemaName + " should have type or reference");
            });
        }
    }

    private boolean isHttpMethod(String method) {
        return method.equals("get") || method.equals("post") || method.equals("put") || 
               method.equals("delete") || method.equals("patch") || method.equals("head") || 
               method.equals("options") || method.equals("trace");
    }
}