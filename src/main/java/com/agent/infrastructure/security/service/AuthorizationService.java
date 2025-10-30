package com.agent.infrastructure.security.service;

import com.agent.domain.model.Permission;
import com.agent.domain.model.Role;
import com.agent.domain.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

/**
 * Service for handling authorization and access control
 */
@Service
public class AuthorizationService {

    private final CustomUserDetailsService userDetailsService;

    public AuthorizationService(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Check if current user has specific permission
     */
    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(permission));
    }

    /**
     * Check if current user has specific role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Check if current user can access conversation
     */
    public boolean canAccessConversation(String conversationId, String userId) {
        // Check if user is admin
        if (hasRole("ADMIN")) {
            return true;
        }

        // Check if user owns the conversation
        String currentUserId = getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(userId)) {
            return true;
        }

        // Check if user has conversation:read permission for any conversation
        return hasPermission("conversation:read");
    }

    /**
     * Check if current user can modify conversation
     */
    public boolean canModifyConversation(String conversationId, String userId) {
        // Check if user is admin
        if (hasRole("ADMIN")) {
            return true;
        }

        // Check if user owns the conversation
        String currentUserId = getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(userId)) {
            return hasPermission("conversation:write");
        }

        return false;
    }

    /**
     * Check if current user can execute specific tool
     */
    public boolean canExecuteTool(String toolName) {
        // Check if user has general tool execution permission
        if (!hasPermission("tool:execute")) {
            return false;
        }

        // Check for tool-specific permissions
        String toolPermission = "tool:" + toolName + ":execute";
        return hasPermission(toolPermission) || hasRole("ADMIN");
    }

    /**
     * Check if current user can access admin endpoints
     */
    public boolean canAccessAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Get current user ID from security context
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        
        // Handle API key authentication
        if (username.startsWith("api-key:")) {
            return username.substring(8); // Remove "api-key:" prefix
        }

        // Handle regular user authentication
        Optional<User> user = userDetailsService.findByUsername(username);
        return user.map(User::getId).orElse(null);
    }

    /**
     * Get current user from security context
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String username = authentication.getName();
        
        // Skip API key authentication
        if (username.startsWith("api-key:")) {
            return Optional.empty();
        }

        return userDetailsService.findByUsername(username);
    }

    /**
     * Check if current authentication is API key based
     */
    public boolean isApiKeyAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getName().startsWith("api-key:");
    }

    /**
     * Check if user has any of the specified permissions
     */
    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all of the specified permissions
     */
    public boolean hasAllPermissions(String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }
}