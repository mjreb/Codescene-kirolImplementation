package com.agent.infrastructure.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for audit logging of sensitive operations
 */
@Service
public class AuditLoggingService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private final ObjectMapper objectMapper;
    private final AuthorizationService authorizationService;

    public AuditLoggingService(ObjectMapper objectMapper, AuthorizationService authorizationService) {
        this.objectMapper = objectMapper;
        this.authorizationService = authorizationService;
    }

    /**
     * Log authentication events
     */
    public void logAuthentication(String username, String authMethod, boolean success, String ipAddress) {
        AuditEvent event = AuditEvent.builder()
                .eventType("AUTHENTICATION")
                .action(success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE")
                .username(username)
                .ipAddress(ipAddress)
                .details(Map.of(
                    "authMethod", authMethod,
                    "success", success
                ))
                .build();
        
        logEvent(event);
    }

    /**
     * Log authorization events
     */
    public void logAuthorization(String resource, String action, boolean granted, String reason) {
        AuditEvent event = AuditEvent.builder()
                .eventType("AUTHORIZATION")
                .action(granted ? "ACCESS_GRANTED" : "ACCESS_DENIED")
                .username(getCurrentUsername())
                .userId(authorizationService.getCurrentUserId())
                .resource(resource)
                .details(Map.of(
                    "action", action,
                    "granted", granted,
                    "reason", reason != null ? reason : ""
                ))
                .build();
        
        logEvent(event);
    }

    /**
     * Log conversation operations
     */
    public void logConversationOperation(String conversationId, String operation, Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
                .eventType("CONVERSATION")
                .action(operation)
                .username(getCurrentUsername())
                .userId(authorizationService.getCurrentUserId())
                .resource("conversation:" + conversationId)
                .details(details != null ? details : new HashMap<>())
                .build();
        
        logEvent(event);
    }

    /**
     * Log tool execution
     */
    public void logToolExecution(String toolName, boolean success, String error) {
        AuditEvent event = AuditEvent.builder()
                .eventType("TOOL_EXECUTION")
                .action(success ? "TOOL_EXECUTED" : "TOOL_FAILED")
                .username(getCurrentUsername())
                .userId(authorizationService.getCurrentUserId())
                .resource("tool:" + toolName)
                .details(Map.of(
                    "toolName", toolName,
                    "success", success,
                    "error", error != null ? error : ""
                ))
                .build();
        
        logEvent(event);
    }

    /**
     * Log API key operations
     */
    public void logApiKeyOperation(String apiKeyId, String operation, Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
                .eventType("API_KEY")
                .action(operation)
                .username(getCurrentUsername())
                .userId(authorizationService.getCurrentUserId())
                .resource("api_key:" + apiKeyId)
                .details(details != null ? details : new HashMap<>())
                .build();
        
        logEvent(event);
    }

    /**
     * Log security violations
     */
    public void logSecurityViolation(String violationType, String description, String ipAddress) {
        AuditEvent event = AuditEvent.builder()
                .eventType("SECURITY_VIOLATION")
                .action(violationType)
                .username(getCurrentUsername())
                .userId(authorizationService.getCurrentUserId())
                .ipAddress(ipAddress)
                .details(Map.of(
                    "violationType", violationType,
                    "description", description
                ))
                .build();
        
        logEvent(event);
    }

    /**
     * Log rate limit violations
     */
    public void logRateLimitViolation(String limitType, int limit, int current, String ipAddress) {
        AuditEvent event = AuditEvent.builder()
                .eventType("RATE_LIMIT_VIOLATION")
                .action("RATE_LIMIT_EXCEEDED")
                .username(getCurrentUsername())
                .userId(authorizationService.getCurrentUserId())
                .ipAddress(ipAddress)
                .details(Map.of(
                    "limitType", limitType,
                    "limit", limit,
                    "current", current
                ))
                .build();
        
        logEvent(event);
    }

    /**
     * Log administrative operations
     */
    public void logAdminOperation(String operation, String resource, Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
                .eventType("ADMIN_OPERATION")
                .action(operation)
                .username(getCurrentUsername())
                .userId(authorizationService.getCurrentUserId())
                .resource(resource)
                .details(details != null ? details : new HashMap<>())
                .build();
        
        logEvent(event);
    }

    private void logEvent(AuditEvent event) {
        try {
            // Set correlation ID for tracing
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
                MDC.put("correlationId", correlationId);
            }
            event.setCorrelationId(correlationId);

            String jsonEvent = objectMapper.writeValueAsString(event);
            auditLogger.info(jsonEvent);
        } catch (Exception e) {
            // Fallback logging if JSON serialization fails
            auditLogger.error("Failed to serialize audit event: {}", event, e);
        }
    }

    private String getCurrentUsername() {
        return authorizationService.getCurrentUser()
                .map(user -> user.getUsername())
                .orElse("anonymous");
    }

    /**
     * Audit event data structure
     */
    public static class AuditEvent {
        private String eventId;
        private Instant timestamp;
        private String eventType;
        private String action;
        private String username;
        private String userId;
        private String resource;
        private String ipAddress;
        private String correlationId;
        private Map<String, Object> details;

        public AuditEvent() {
            this.eventId = UUID.randomUUID().toString();
            this.timestamp = Instant.now();
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }

        public static class Builder {
            private final AuditEvent event = new AuditEvent();

            public Builder eventType(String eventType) {
                event.setEventType(eventType);
                return this;
            }

            public Builder action(String action) {
                event.setAction(action);
                return this;
            }

            public Builder username(String username) {
                event.setUsername(username);
                return this;
            }

            public Builder userId(String userId) {
                event.setUserId(userId);
                return this;
            }

            public Builder resource(String resource) {
                event.setResource(resource);
                return this;
            }

            public Builder ipAddress(String ipAddress) {
                event.setIpAddress(ipAddress);
                return this;
            }

            public Builder details(Map<String, Object> details) {
                event.setDetails(details);
                return this;
            }

            public AuditEvent build() {
                return event;
            }
        }
    }
}