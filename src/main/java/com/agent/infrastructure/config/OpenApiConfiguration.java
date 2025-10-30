package com.agent.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Agent Application.
 * Provides comprehensive API documentation with security schemes and examples.
 */
@Configuration
public class OpenApiConfiguration {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:Intelligent Agent Application with ReAct Pattern}")
    private String appDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agent Application API")
                        .version(appVersion)
                        .description(appDescription + "\n\n" +
                                "This API provides endpoints for interacting with intelligent agents that implement " +
                                "the ReAct (Reasoning and Acting) pattern. The agents can process natural language " +
                                "requests, reason about tasks, and execute actions using various tools and LLM providers.")
                        .contact(new Contact()
                                .name("Agent Application Team")
                                .email("support@agent-app.com")
                                .url("https://github.com/agent-app/agent-application"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("Development server"),
                        new Server()
                                .url("https://api.agent-app.com" + contextPath)
                                .description("Production server")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("apiKeyAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from /auth/login endpoint"))
                        .addSecuritySchemes("apiKeyAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("API key for service-to-service authentication")))
                .tags(List.of(
                        new Tag().name("Authentication").description("User authentication and authorization"),
                        new Tag().name("Conversations").description("Agent conversation management"),
                        new Tag().name("Streaming").description("Real-time conversation streaming"),
                        new Tag().name("Monitoring").description("System monitoring and health checks"),
                        new Tag().name("System Status").description("System status and operational information")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public API")
                .pathsToMatch("/conversations/**", "/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin API")
                .pathsToMatch("/admin/**", "/actuator/**")
                .build();
    }

    @Bean
    public GroupedOpenApi monitoringApi() {
        return GroupedOpenApi.builder()
                .group("monitoring")
                .displayName("Monitoring API")
                .pathsToMatch("/health/**", "/metrics/**", "/status/**")
                .build();
    }

    @Bean
    public GroupedOpenApi streamingApi() {
        return GroupedOpenApi.builder()
                .group("streaming")
                .displayName("Streaming API")
                .pathsToMatch("/stream/**", "/ws/**")
                .build();
    }
}