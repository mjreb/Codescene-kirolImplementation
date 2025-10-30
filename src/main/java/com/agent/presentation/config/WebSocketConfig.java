package com.agent.presentation.config;

import com.agent.presentation.websocket.ConversationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time conversation updates.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final ConversationWebSocketHandler conversationWebSocketHandler;
    
    public WebSocketConfig(ConversationWebSocketHandler conversationWebSocketHandler) {
        this.conversationWebSocketHandler = conversationWebSocketHandler;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(conversationWebSocketHandler, "/ws/conversations")
                .setAllowedOrigins("*"); // Configure appropriately for production
    }
}