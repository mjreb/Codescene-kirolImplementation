package com.agent.infrastructure.security.service;

import com.agent.domain.model.Permission;
import com.agent.domain.model.Role;
import com.agent.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private SecurityContext securityContext;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(userDetailsService);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldReturnTrueWhenUserHasPermission() {
        // Given
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("conversation:read"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean hasPermission = authorizationService.hasPermission("conversation:read");

        // Then
        assertTrue(hasPermission);
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotHavePermission() {
        // Given
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("conversation:read"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean hasPermission = authorizationService.hasPermission("conversation:write");

        // Then
        assertFalse(hasPermission);
    }

    @Test
    void shouldReturnTrueWhenUserHasRole() {
        // Given
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean hasRole = authorizationService.hasRole("ADMIN");

        // Then
        assertTrue(hasRole);
    }

    @Test
    void shouldAllowAdminToAccessAnyConversation() {
        // Given
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean canAccess = authorizationService.canAccessConversation("conv-123", "other-user");

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldAllowUserToAccessOwnConversation() {
        // Given
        User user = new User("user-123", "user@example.com", "user@example.com");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("conversation:read"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userDetailsService.findByUsername("user@example.com")).thenReturn(Optional.of(user));

        // When
        boolean canAccess = authorizationService.canAccessConversation("conv-123", "user-123");

        // Then
        assertTrue(canAccess);
    }

    @Test
    void shouldDenyUserAccessToOtherUsersConversation() {
        // Given
        User user = new User("user-123", "user@example.com", "user@example.com");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("conversation:read"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userDetailsService.findByUsername("user@example.com")).thenReturn(Optional.of(user));

        // When
        boolean canAccess = authorizationService.canAccessConversation("conv-123", "other-user");

        // Then
        assertFalse(canAccess);
    }

    @Test
    void shouldAllowToolExecutionWithPermission() {
        // Given
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("tool:execute"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean canExecute = authorizationService.canExecuteTool("calculator");

        // Then
        assertTrue(canExecute);
    }

    @Test
    void shouldDenyToolExecutionWithoutPermission() {
        // Given
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("conversation:read"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean canExecute = authorizationService.canExecuteTool("calculator");

        // Then
        assertFalse(canExecute);
    }

    @Test
    void shouldIdentifyApiKeyAuthentication() {
        // Given
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "api-key:123", 
                null, 
                List.of(new SimpleGrantedAuthority("SCOPE_conversation:read"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean isApiKey = authorizationService.isApiKeyAuthentication();

        // Then
        assertTrue(isApiKey);
    }

    @Test
    void shouldReturnCurrentUserWhenAuthenticated() {
        // Given
        User user = new User("user-123", "user@example.com", "user@example.com");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com", 
                null, 
                List.of(new SimpleGrantedAuthority("conversation:read"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(userDetailsService.findByUsername("user@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> currentUser = authorizationService.getCurrentUser();

        // Then
        assertTrue(currentUser.isPresent());
        assertEquals("user@example.com", currentUser.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenNotAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        Optional<User> currentUser = authorizationService.getCurrentUser();

        // Then
        assertFalse(currentUser.isPresent());
    }
}