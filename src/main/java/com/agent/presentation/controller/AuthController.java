package com.agent.presentation.controller;

import com.agent.infrastructure.security.JwtService;
import com.agent.infrastructure.security.service.ApiKeyService;
import com.agent.infrastructure.security.service.CustomUserDetailsService;
import com.agent.presentation.dto.ErrorResponse;
import com.agent.presentation.dto.auth.AuthenticationRequest;
import com.agent.presentation.dto.auth.AuthenticationResponse;
import com.agent.presentation.dto.auth.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Authentication controller for JWT and API key authentication
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final ApiKeyService apiKeyService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            ApiKeyService apiKeyService,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.apiKeyService = apiKeyService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate user and return JWT token
     */
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates a user with email and password, returning JWT access and refresh tokens. " +
                     "The access token should be included in the Authorization header for subsequent API calls.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User credentials",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationRequest.class),
                examples = @ExampleObject(
                    name = "Login example",
                    value = """
                        {
                          "email": "user@example.com",
                          "password": "securePassword123"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class),
                examples = @ExampleObject(
                    name = "Successful authentication",
                    value = """
                        {
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "tokenType": "Bearer",
                          "expiresIn": 86400
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails user = userDetailsService.loadUserByUsername(request.getEmail());
        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400) // 24 hours
                .build());
    }

    /**
     * Register new user
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        // Check if user already exists
        if (userDetailsService.findByUsername(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        // Create new user
        var user = new com.agent.domain.model.User(
                java.util.UUID.randomUUID().toString(),
                request.getEmail(),
                request.getEmail()
        );
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        // Assign default role
        var userRole = new com.agent.domain.model.Role("1", "USER", "Standard user role");
        user.setRoles(Set.of(userRole));
        
        userDetailsService.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return ResponseEntity.ok(AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400) // 24 hours
                .build());
    }

    /**
     * Refresh JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestHeader("Authorization") String refreshToken
    ) {
        if (refreshToken == null || !refreshToken.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String token = refreshToken.substring(7);
        String username = jwtService.extractUsername(token);
        
        if (username != null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtService.isTokenValid(token, userDetails)) {
                String newAccessToken = jwtService.generateToken(userDetails);
                
                return ResponseEntity.ok(AuthenticationResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(token)
                        .tokenType("Bearer")
                        .expiresIn(86400) // 24 hours
                        .build());
            }
        }
        
        return ResponseEntity.badRequest().build();
    }

    /**
     * Generate API key for authenticated user
     */
    @PostMapping("/api-key")
    public ResponseEntity<String> generateApiKey(
            @RequestParam String name,
            @RequestParam(required = false) Set<String> scopes
    ) {
        // In a real implementation, get user ID from security context
        String userId = "default-user";
        
        if (scopes == null) {
            scopes = Set.of("conversation:read", "conversation:write");
        }
        
        Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS); // 1 year
        String apiKey = apiKeyService.generateApiKey(userId, name, scopes, expiresAt);
        
        return ResponseEntity.ok(apiKey);
    }
}