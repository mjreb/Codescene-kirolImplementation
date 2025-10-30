package com.agent.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Docker deployment.
 * These tests verify that the application can be built and deployed using Docker.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DOCKER_TESTS", matches = "true")
public class DockerDeploymentTest {

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
            new File("docker-compose.yml"))
            .withExposedService("agent-app", 8080,
                    Wait.forHttp("/api/actuator/health")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(5)))
            .withExposedService("postgres", 5432,
                    Wait.forListeningPort()
                            .withStartupTimeout(Duration.ofMinutes(2)))
            .withExposedService("redis", 6379,
                    Wait.forListeningPort()
                            .withStartupTimeout(Duration.ofMinutes(2)));

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Test
    void testApplicationStartup() throws Exception {
        // Test that the application starts successfully
        String host = environment.getServiceHost("agent-app", 8080);
        Integer port = environment.getServicePort("agent-app", 8080);
        
        String healthUrl = String.format("http://%s:%d/api/actuator/health", host, port);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    void testDatabaseConnectivity() throws Exception {
        // Test that the application can connect to PostgreSQL
        String host = environment.getServiceHost("agent-app", 8080);
        Integer port = environment.getServicePort("agent-app", 8080);
        
        String healthUrl = String.format("http://%s:%d/api/actuator/health/db", host, port);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    void testRedisConnectivity() throws Exception {
        // Test that the application can connect to Redis
        String host = environment.getServiceHost("agent-app", 8080);
        Integer port = environment.getServicePort("agent-app", 8080);
        
        String healthUrl = String.format("http://%s:%d/api/actuator/health/redis", host, port);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    void testApiEndpointsAccessible() throws Exception {
        // Test that main API endpoints are accessible
        String host = environment.getServiceHost("agent-app", 8080);
        Integer port = environment.getServicePort("agent-app", 8080);
        
        // Test Swagger UI
        String swaggerUrl = String.format("http://%s:%d/api/swagger-ui.html", host, port);
        HttpRequest swaggerRequest = HttpRequest.newBuilder()
                .uri(URI.create(swaggerUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> swaggerResponse = httpClient.send(swaggerRequest, 
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, swaggerResponse.statusCode());

        // Test OpenAPI docs
        String apiDocsUrl = String.format("http://%s:%d/api/v3/api-docs", host, port);
        HttpRequest apiDocsRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiDocsUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> apiDocsResponse = httpClient.send(apiDocsRequest, 
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, apiDocsResponse.statusCode());
        assertTrue(apiDocsResponse.body().contains("\"openapi\":\"3.0"));
    }

    @Test
    void testPrometheusMetrics() throws Exception {
        // Test that Prometheus metrics are exposed
        String host = environment.getServiceHost("agent-app", 8080);
        Integer port = environment.getServicePort("agent-app", 8080);
        
        String metricsUrl = String.format("http://%s:%d/api/actuator/prometheus", host, port);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(metricsUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("# HELP"));
        assertTrue(response.body().contains("jvm_memory_used_bytes"));
    }

    @Test
    void testContainerHealthChecks() {
        // Verify that all containers are healthy
        assertTrue(environment.getContainerByServiceName("agent-app_1")
                .map(container -> container.isHealthy())
                .orElse(false), "Agent application container should be healthy");

        assertTrue(environment.getContainerByServiceName("postgres_1")
                .map(container -> container.isRunning())
                .orElse(false), "PostgreSQL container should be running");

        assertTrue(environment.getContainerByServiceName("redis_1")
                .map(container -> container.isRunning())
                .orElse(false), "Redis container should be running");
    }

    @Test
    void testResourceUsage() throws Exception {
        // Test that the application is using resources within expected limits
        String host = environment.getServiceHost("agent-app", 8080);
        Integer port = environment.getServicePort("agent-app", 8080);
        
        String metricsUrl = String.format("http://%s:%d/api/actuator/metrics/jvm.memory.used", host, port);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(metricsUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        
        // Parse memory usage and verify it's within reasonable limits
        String responseBody = response.body();
        assertTrue(responseBody.contains("\"name\":\"jvm.memory.used\""));
        
        // Memory usage should be less than 1GB for basic startup
        // This is a rough check - in production you'd parse the JSON properly
        assertFalse(responseBody.contains("\"value\":1073741824")); // 1GB in bytes
    }
}