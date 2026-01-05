package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.AuthenticationRequest;
import com.talentica.paymentgateway.dto.AuthenticationResponse;
import com.talentica.paymentgateway.dto.RegistrationRequest;
import com.talentica.paymentgateway.entity.User;
import com.talentica.paymentgateway.service.JwtService;
import com.talentica.paymentgateway.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("POST /auth/login returns tokens on valid credentials")
    void login_ok() {
        // Given
        AuthenticationRequest req = new AuthenticationRequest("user@example.com", "Password123!");
        
        when(userService.validateCredentials(anyString(), anyString())).thenReturn(true);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setIsActive(true);
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(user));
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com").password("x").roles("USER").build();
        when(userService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh");

        // When
        ResponseEntity<?> response = authController.login(req);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        AuthenticationResponse authResponse = (AuthenticationResponse) response.getBody();
        assertEquals("access", authResponse.getAccessToken());
        assertEquals("refresh", authResponse.getRefreshToken());
        assertEquals("user@example.com", authResponse.getEmail());
    }

    @Test
    @DisplayName("POST /auth/login returns 401 on invalid credentials")
    void login_invalidCredentials() {
        // Given
        AuthenticationRequest req = new AuthenticationRequest("user@example.com", "bad");
        when(userService.validateCredentials(anyString(), anyString())).thenReturn(false);

        // When
        ResponseEntity<?> response = authController.login(req);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /auth/register returns 201 with tokens")
    void register_ok() {
        // Given
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("user@example.com");
        req.setPassword("Password123!");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        when(userService.registerUser(any(RegistrationRequest.class))).thenReturn(user);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com").password("x").roles("USER").build();
        when(userService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh");

        // When
        ResponseEntity<?> response = authController.register(req);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        AuthenticationResponse authResponse = (AuthenticationResponse) response.getBody();
        assertEquals("access", authResponse.getAccessToken());
        assertEquals("refresh", authResponse.getRefreshToken());
    }

    @Test
    @DisplayName("POST /auth/refresh returns new access token when valid")
    void refresh_ok() {
        // Given
        Map<String, String> refreshRequest = Map.of("refresh_token", "refresh");
        
        when(jwtService.isRefreshToken(eq("refresh"))).thenReturn(true);
        when(jwtService.extractUsername(eq("refresh"))).thenReturn("user@example.com");
        when(userService.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com").password("x").roles("USER").build();
        when(userService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.isTokenValid(eq("refresh"), any(UserDetails.class))).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("new-access");

        // When
        ResponseEntity<?> response = authController.refresh(refreshRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // The refresh endpoint returns a Map, not AuthenticationResponse
        @SuppressWarnings("unchecked")
        Map<String, String> refreshResponse = (Map<String, String>) response.getBody();
        assertEquals("new-access", refreshResponse.get("access_token"));
        assertEquals("Bearer", refreshResponse.get("token_type"));
    }
}
