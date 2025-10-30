package com.agent.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "mySecretKeyThatShouldBeChangedInProductionAndMustBeAtLeast256BitsLong");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void shouldGenerateValidToken() {
        String token = jwtService.generateToken(userDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);
        
        assertEquals(userDetails.getUsername(), extractedUsername);
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        String token = jwtService.generateToken(userDetails);
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        
        assertTrue(isValid);
    }

    @Test
    void shouldRejectInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(Exception.class, () -> {
            jwtService.isTokenValid(invalidToken, userDetails);
        });
    }

    @Test
    void shouldGenerateTokenWithExtraClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", "123");
        extraClaims.put("roles", java.util.List.of("USER"));
        
        String token = jwtService.generateToken(extraClaims, userDetails);
        
        assertNotNull(token);
        String extractedUserId = jwtService.extractUserId(token);
        String[] extractedRoles = jwtService.extractRoles(token);
        
        assertEquals("123", extractedUserId);
        assertArrayEquals(new String[]{"USER"}, extractedRoles);
    }

    @Test
    void shouldGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        
        String extractedUsername = jwtService.extractUsername(refreshToken);
        assertEquals(userDetails.getUsername(), extractedUsername);
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(userDetails);
        
        UserDetails differentUser = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        
        boolean isValid = jwtService.isTokenValid(token, differentUser);
        assertFalse(isValid);
    }
}