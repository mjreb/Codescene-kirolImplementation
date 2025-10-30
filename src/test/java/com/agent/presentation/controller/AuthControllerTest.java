package com.agent.presentation.controller;

import com.agent.infrastructure.security.JwtService;
import com.agent.infrastructure.security.service.ApiKeyService;
import com.agent.infrastructure.security.service.CustomUserDetailsService;
import com.agent.presentation.dto.auth.AuthenticationRequest;
import com.agent.presentation.dto.auth.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldAuthenticateValidUser() throws Exception {
        // Given
        AuthenticationRequest request = new AuthenticationRequest("user@example.com", "password");
        UserDetails userDetails = User.builder()
                .username("user@example.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();

        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400));

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        // Given
        AuthenticationRequest request = new AuthenticationRequest("user@example.com", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterNewUser() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "John", "Doe");
        UserDetails userDetails = User.builder()
                .username("user@example.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();

        when(userDetailsService.findByUsername("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(userDetailsService).save(any());
    }

    @Test
    void shouldRejectRegistrationForExistingUser() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123", "John", "Doe");
        when(userDetailsService.findByUsername("existing@example.com"))
                .thenReturn(Optional.of(new com.agent.domain.model.User()));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userDetailsService, never()).save(any());
    }

    @Test
    void shouldRefreshValidToken() throws Exception {
        // Given
        UserDetails userDetails = User.builder()
                .username("user@example.com")
                .password("encoded-password")
                .authorities(Collections.emptyList())
                .build();

        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-refresh-token", userDetails)).thenReturn(true);
        when(jwtService.generateToken(userDetails)).thenReturn("new-jwt-token");

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer valid-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("valid-refresh-token"));
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGenerateApiKey() throws Exception {
        // Given
        when(apiKeyService.generateApiKey(anyString(), anyString(), any(), any()))
                .thenReturn("generated-api-key");

        // When & Then
        mockMvc.perform(post("/api/auth/api-key")
                .param("name", "Test API Key"))
                .andExpect(status().isOk())
                .andExpect(content().string("generated-api-key"));

        verify(apiKeyService).generateApiKey(eq("default-user"), eq("Test API Key"), any(), any());
    }

    @Test
    void shouldValidateAuthenticationRequestFields() throws Exception {
        // Given - invalid email
        AuthenticationRequest invalidRequest = new AuthenticationRequest("invalid-email", "password");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldValidateRegistrationRequestFields() throws Exception {
        // Given - password too short
        RegisterRequest invalidRequest = new RegisterRequest("user@example.com", "123", "John", "Doe");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}