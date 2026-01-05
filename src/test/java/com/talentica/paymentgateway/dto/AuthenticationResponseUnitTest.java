package com.talentica.paymentgateway.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthenticationResponse DTO.
 * Tests constructors, getters, setters, JSON serialization, and field validation.
 */
@DisplayName("AuthenticationResponse Unit Tests")
class AuthenticationResponseUnitTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should create AuthenticationResponse with default constructor")
    void shouldCreateWithDefaultConstructor() {
        // When
        AuthenticationResponse response = new AuthenticationResponse();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(0L);
        assertThat(response.getUserId()).isNull();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getRoles()).isNull();
    }

    @Test
    @DisplayName("Should create AuthenticationResponse with parameterized constructor")
    void shouldCreateWithParameterizedConstructor() {
        // Given
        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-456";
        long expiresIn = 3600L;
        String userId = "user-123";
        String email = "test@example.com";
        String[] roles = {"ROLE_USER", "ROLE_ADMIN"};

        // When
        AuthenticationResponse response = AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(expiresIn)
            .userId(userId)
            .email(email)
            .roles(roles)
            .tokenType("Bearer")
            .build();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(expiresIn);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getRoles()).isEqualTo(roles);
    }

    @Test
    @DisplayName("Should set and get access token")
    void shouldSetAndGetAccessToken() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        String accessToken = "new-access-token";

        // When
        response.setAccessToken(accessToken);

        // Then
        assertThat(response.getAccessToken()).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("Should set and get refresh token")
    void shouldSetAndGetRefreshToken() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        String refreshToken = "new-refresh-token";

        // When
        response.setRefreshToken(refreshToken);

        // Then
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Should set and get token type")
    void shouldSetAndGetTokenType() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        String tokenType = "JWT";

        // When
        response.setTokenType(tokenType);

        // Then
        assertThat(response.getTokenType()).isEqualTo(tokenType);
    }

    @Test
    @DisplayName("Should set and get expires in")
    void shouldSetAndGetExpiresIn() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        long expiresIn = 7200L;

        // When
        response.setExpiresIn(expiresIn);

        // Then
        assertThat(response.getExpiresIn()).isEqualTo(expiresIn);
    }

    @Test
    @DisplayName("Should set and get user ID")
    void shouldSetAndGetUserId() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        String userId = "user-456";

        // When
        response.setUserId(userId);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should set and get email")
    void shouldSetAndGetEmail() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        String email = "new@example.com";

        // When
        response.setEmail(email);

        // Then
        assertThat(response.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Should set and get roles")
    void shouldSetAndGetRoles() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        String[] roles = {"ROLE_MERCHANT", "ROLE_USER"};

        // When
        response.setRoles(roles);

        // Then
        assertThat(response.getRoles()).isEqualTo(roles);
    }

    @Test
    @DisplayName("Should handle null values properly")
    void shouldHandleNullValues() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();

        // When
        response.setAccessToken(null);
        response.setRefreshToken(null);
        response.setTokenType(null);
        response.setUserId(null);
        response.setEmail(null);
        response.setRoles(null);

        // Then
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getTokenType()).isNull();
        assertThat(response.getUserId()).isNull();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getRoles()).isNull();
    }

    @Test
    @DisplayName("Should serialize to JSON correctly")
    void shouldSerializeToJsonCorrectly() throws Exception {
        // Given
        AuthenticationResponse response = AuthenticationResponse.builder()
            .accessToken("access-123")
            .refreshToken("refresh-456")
            .expiresIn(3600L)
            .userId("user-789")
            .email("test@example.com")
            .roles(new String[]{"ROLE_USER"})
            .tokenType("Bearer")
            .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).contains("\"access_token\":\"access-123\"");
        assertThat(json).contains("\"refresh_token\":\"refresh-456\"");
        assertThat(json).contains("\"token_type\":\"Bearer\"");
        assertThat(json).contains("\"expires_in\":3600");
        assertThat(json).contains("\"user_id\":\"user-789\"");
        assertThat(json).contains("\"email\":\"test@example.com\"");
        assertThat(json).contains("\"roles\":[\"ROLE_USER\"]");
    }

    @Test
    @DisplayName("Should deserialize from JSON correctly")
    void shouldDeserializeFromJsonCorrectly() throws Exception {
        // Given
        String json = """
            {
                "access_token": "access-123",
                "refresh_token": "refresh-456",
                "token_type": "Bearer",
                "expires_in": 3600,
                "user_id": "user-789",
                "email": "test@example.com",
                "roles": ["ROLE_USER", "ROLE_ADMIN"]
            }
            """;

        // When
        AuthenticationResponse response = objectMapper.readValue(json, AuthenticationResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-456");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getUserId()).isEqualTo("user-789");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRoles()).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should handle empty roles array")
    void shouldHandleEmptyRolesArray() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        String[] emptyRoles = {};

        // When
        response.setRoles(emptyRoles);

        // Then
        assertThat(response.getRoles()).isNotNull();
        assertThat(response.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("Should handle large expires in value")
    void shouldHandleLargeExpiresInValue() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        long largeExpiresIn = Long.MAX_VALUE;

        // When
        response.setExpiresIn(largeExpiresIn);

        // Then
        assertThat(response.getExpiresIn()).isEqualTo(largeExpiresIn);
    }

    @Test
    @DisplayName("Should handle negative expires in value")
    void shouldHandleNegativeExpiresInValue() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        long negativeExpiresIn = -1L;

        // When
        response.setExpiresIn(negativeExpiresIn);

        // Then
        assertThat(response.getExpiresIn()).isEqualTo(negativeExpiresIn);
    }

    @Test
    @DisplayName("Should maintain default token type when not set")
    void shouldMaintainDefaultTokenType() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();

        // When & Then
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        
        // When setting other fields
        response.setAccessToken("token");
        response.setUserId("user");
        
        // Then token type should remain default
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Should handle multiple role assignments")
    void shouldHandleMultipleRoleAssignments() {
        // Given
        AuthenticationResponse response = new AuthenticationResponse();
        String[] firstRoles = {"ROLE_USER"};
        String[] secondRoles = {"ROLE_ADMIN", "ROLE_MERCHANT", "ROLE_SUPER_ADMIN"};

        // When
        response.setRoles(firstRoles);
        assertThat(response.getRoles()).containsExactly("ROLE_USER");

        response.setRoles(secondRoles);

        // Then
        assertThat(response.getRoles()).containsExactly("ROLE_ADMIN", "ROLE_MERCHANT", "ROLE_SUPER_ADMIN");
    }
}
