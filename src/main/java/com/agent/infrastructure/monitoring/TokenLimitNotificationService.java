package com.agent.infrastructure.monitoring;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling token limit notifications and user communication.
 */
@Service
public class TokenLimitNotificationService {
    
    // Track when users were last notified to avoid spam
    private final Map<String, Long> lastNotificationTime = new ConcurrentHashMap<>();
    
    // Minimum time between notifications (in milliseconds) - 1 hour
    private static final long NOTIFICATION_COOLDOWN = 60 * 60 * 1000;
    
    /**
     * Send notification when token limits are exceeded.
     */
    public void notifyTokenLimitExceeded(TokenLimitException exception) {
        String userId = exception.getUserId();
        if (userId == null) {
            return;
        }
        
        // Check if we should send notification (cooldown period)
        if (shouldSendNotification(userId)) {
            sendLimitExceededNotification(exception);
            updateLastNotificationTime(userId);
        }
    }
    
    /**
     * Send warning notification when approaching limits.
     */
    public void notifyApproachingLimit(String userId, TokenLimitEnforcementService.TokenLimitWarning warning) {
        if (warning.getWarningLevel() == TokenLimitEnforcementService.TokenLimitWarning.WarningLevel.NONE) {
            return;
        }
        
        // Only send high and critical warnings
        if (warning.getWarningLevel().ordinal() >= TokenLimitEnforcementService.TokenLimitWarning.WarningLevel.HIGH.ordinal()) {
            if (shouldSendNotification(userId)) {
                sendWarningNotification(userId, warning);
                updateLastNotificationTime(userId);
            }
        }
    }
    
    /**
     * Create user-friendly error message for token limit exceptions.
     */
    public Map<String, Object> createLimitExceededResponse(TokenLimitException exception) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("error", "TOKEN_LIMIT_EXCEEDED");
        response.put("message", createUserFriendlyMessage(exception));
        response.put("limitType", exception.getLimitType());
        response.put("currentUsage", exception.getCurrentUsage());
        response.put("limit", exception.getLimit());
        
        // Add suggestions based on limit type
        response.put("suggestions", createSuggestions(exception));
        
        return response;
    }
    
    /**
     * Create user-friendly warning message.
     */
    public Map<String, Object> createWarningResponse(TokenLimitEnforcementService.TokenLimitWarning warning) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("warning", "APPROACHING_TOKEN_LIMIT");
        response.put("level", warning.getWarningLevel().name());
        response.put("messages", warning.getWarnings());
        response.put("suggestions", createWarningSuggestions(warning));
        
        return response;
    }
    
    // Private helper methods
    
    private boolean shouldSendNotification(String userId) {
        Long lastNotified = lastNotificationTime.get(userId);
        if (lastNotified == null) {
            return true;
        }
        
        return System.currentTimeMillis() - lastNotified > NOTIFICATION_COOLDOWN;
    }
    
    private void updateLastNotificationTime(String userId) {
        lastNotificationTime.put(userId, System.currentTimeMillis());
    }
    
    private void sendLimitExceededNotification(TokenLimitException exception) {
        // In a real implementation, this would send notifications via:
        // - Email
        // - Push notifications
        // - In-app notifications
        // - Webhook to external systems
        
        System.out.println("TOKEN LIMIT EXCEEDED NOTIFICATION:");
        System.out.println("User: " + exception.getUserId());
        System.out.println("Limit Type: " + exception.getLimitType());
        System.out.println("Message: " + exception.getMessage());
    }
    
    private void sendWarningNotification(String userId, TokenLimitEnforcementService.TokenLimitWarning warning) {
        // In a real implementation, this would send warning notifications
        
        System.out.println("TOKEN LIMIT WARNING NOTIFICATION:");
        System.out.println("User: " + userId);
        System.out.println("Warning Level: " + warning.getWarningLevel());
        System.out.println("Messages: " + String.join(", ", warning.getWarnings()));
    }
    
    private String createUserFriendlyMessage(TokenLimitException exception) {
        switch (exception.getLimitType()) {
            case "DAILY":
                return "You have reached your daily token limit. Please try again tomorrow or upgrade your plan.";
            case "MONTHLY":
                return "You have reached your monthly token limit. Please upgrade your plan or wait until next month.";
            case "CONVERSATION":
                return "This conversation has reached the maximum token limit. Please start a new conversation.";
            case "REQUEST":
                return "Your request is too large. Please try breaking it into smaller parts.";
            default:
                return "Token limit exceeded. Please reduce your usage or upgrade your plan.";
        }
    }
    
    private String[] createSuggestions(TokenLimitException exception) {
        switch (exception.getLimitType()) {
            case "DAILY":
                return new String[]{
                    "Wait until tomorrow when your daily limit resets",
                    "Upgrade to a higher tier plan for increased limits",
                    "Use shorter messages to conserve tokens"
                };
            case "MONTHLY":
                return new String[]{
                    "Upgrade to a higher tier plan",
                    "Wait until next month when your limit resets",
                    "Review your usage patterns to optimize token consumption"
                };
            case "CONVERSATION":
                return new String[]{
                    "Start a new conversation",
                    "Summarize the current conversation and continue in a new one",
                    "Use more concise messages"
                };
            case "REQUEST":
                return new String[]{
                    "Break your request into smaller parts",
                    "Use more concise language",
                    "Remove unnecessary details from your message"
                };
            default:
                return new String[]{
                    "Reduce your token usage",
                    "Consider upgrading your plan"
                };
        }
    }
    
    private String[] createWarningSuggestions(TokenLimitEnforcementService.TokenLimitWarning warning) {
        return new String[]{
            "Consider upgrading your plan for higher limits",
            "Monitor your usage more closely",
            "Use more concise messages to conserve tokens",
            "Review your conversation patterns for optimization opportunities"
        };
    }
}