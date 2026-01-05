package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.AuthenticationRequest;
import com.talentica.paymentgateway.dto.AuthenticationResponse;
import com.talentica.paymentgateway.dto.RegistrationRequest;
import com.talentica.paymentgateway.entity.User;
import com.talentica.paymentgateway.service.JwtService;
import com.talentica.paymentgateway.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Authentication Controller handling login, registration, and token refresh.
 * Provides JWT-based authentication endpoints for the payment gateway.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and token management")
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * User login endpoint.
     * 
     * @param request Authentication request containing email and password
     * @return Authentication response with JWT tokens
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest request) {
        try {
            log.info("=== LOGIN ATTEMPT START ===");
            log.info("Login attempt for email: {}", request.getEmail());
            log.debug("Request details - Email: {}, Password length: {}", 
                        request.getEmail(), request.getPassword() != null ? request.getPassword().length() : 0);

            // Validate user credentials using UserService
            log.debug("Step 1: Validating user credentials...");
            boolean credentialsValid = userService.validateCredentials(request.getEmail(), request.getPassword());
            log.info("Credentials validation result: {}", credentialsValid);
            
            if (!credentialsValid) {
                log.warn("Invalid login attempt for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
            }

            // Get user from database
            log.debug("Step 2: Fetching user from database...");
            Optional<User> userOpt = userService.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                log.error("User not found after successful credential validation for email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
            }

            User user = userOpt.get();
            log.info("Found user: ID={}, Email={}, Active={}", user.getId(), user.getEmail(), user.getIsActive());

            // Create user details for JWT
            log.debug("Step 3: Loading UserDetails for JWT generation...");
            UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
            log.debug("UserDetails loaded successfully for: {}", userDetails.getUsername());

            // Generate tokens
            log.debug("Step 4: Generating JWT tokens...");
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);
            log.debug("JWT tokens generated successfully");

            // Update last login timestamp
            log.debug("Step 5: Updating last login timestamp...");
            userService.updateLastLogin(user.getId());

            // Determine user roles based on email (simple role assignment)
            log.debug("Step 6: Determining user roles...");
            String[] roles = getUserRoles(user.getEmail());
            log.debug("User roles assigned: {}", java.util.Arrays.toString(roles));

            // Prepare response
            log.debug("Step 7: Preparing authentication response...");
            AuthenticationResponse response = new AuthenticationResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    86400000L, // 24 hours in milliseconds
                    user.getId().toString(),
                    user.getEmail(),
                    roles
            );

            log.info("=== LOGIN SUCCESS === User: {}", request.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("=== LOGIN ERROR === Email: {}, Error: {}", request.getEmail(), e.getMessage(), e);
            log.error("Full stack trace:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed", "details", e.getMessage()));
        }
    }

    /**
     */
    private String[] getUserRoles(String email) {
        String emailLower = email.toLowerCase();
        if (emailLower.startsWith("admin")) {
            return new String[]{"ROLE_ADMIN", "ROLE_USER"};
        } else if (emailLower.startsWith("manager")) {
            return new String[]{"ROLE_MANAGER", "ROLE_USER"};
        } else {
            return new String[]{"ROLE_USER"};
        }
    }

    /**
     * User registration endpoint.
     * 
     * @param request Registration request containing user details
     * @return Registration response with user information
     */
    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Registration successful"),
        @ApiResponse(responseCode = "409", description = "User already exists"),
        @ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
        try {
            log.info("Registration attempt for email: {}", request.getEmail());

            // Register user using UserService
            User user = userService.registerUser(request);

            // Create user details for JWT generation
            UserDetails userDetails = userService.loadUserByUsername(user.getEmail());

            // Generate tokens
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            // Determine user roles based on email
            String[] roles = getUserRoles(user.getEmail());

            // Prepare response
            AuthenticationResponse response = new AuthenticationResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    86400000L, // 24 hours in milliseconds
                    user.getId().toString(),
                    user.getEmail(),
                    roles
            );

            log.info("Successful registration for user: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Registration attempt for existing user: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during registration for email {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed"));
        }
    }

    /**
     * Token refresh endpoint.
     * 
     * @param refreshToken Refresh token
     * @return New access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refresh_token");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Refresh token is required"));
            }

            // Validate refresh token
            if (!jwtService.isRefreshToken(refreshToken)) {
                log.warn("Invalid token type provided for refresh");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }

            String username = jwtService.extractUsername(refreshToken);
            Optional<User> userOpt = userService.findByEmail(username.toLowerCase());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }

            // Create user details using UserService
            UserDetails userDetails = userService.loadUserByUsername(username);

            // Validate refresh token
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }

            // Generate new access token
            String accessToken = jwtService.generateAccessToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", accessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", 86400000L);

            log.info("Token refreshed for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during token refresh: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token refresh failed"));
        }
    }

    /**
     * Get current user information.
     * 
     * @return Current user details
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get authenticated user information")
    public ResponseEntity<?> getCurrentUser() {
        // This endpoint requires authentication - implementation depends on security context
        return ResponseEntity.ok(Map.of("message", "User profile endpoint - requires authentication"));
    }

    /**
     * User logout endpoint.
     * 
     * @return Logout confirmation
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidate user session")
    public ResponseEntity<?> logout() {
        // In a stateless JWT system, logout is typically handled client-side
        // Here you could add token to blacklist if needed
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

}
