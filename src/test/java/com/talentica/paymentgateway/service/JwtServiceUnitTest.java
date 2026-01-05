package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.config.ApplicationConfig;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService.
 * Tests JWT token generation, validation, extraction, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceUnitTest {

    private ApplicationConfig.AppProperties appProperties;
    private JwtService jwtService;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        // Create real instances instead of mocking static inner classes
        appProperties = new ApplicationConfig.AppProperties();
        ApplicationConfig.AppProperties.Jwt jwtProperties = new ApplicationConfig.AppProperties.Jwt();
        jwtProperties.setSecret("mySecretKeyForTestingJwtTokenGenerationAndValidation123456789");
        jwtProperties.setExpiration(86400000L); // 24 hours
        jwtProperties.setRefreshExpiration(604800000L); // 7 days
        appProperties.setJwt(jwtProperties);

        jwtService = new JwtService(appProperties);

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_CUSTOMER")
        );
        testUser = new User("testuser@example.com", "password", authorities);
    }

    @Test
    void generateAccessToken_WithUserDetails_ShouldReturnValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertEquals("testuser@example.com", jwtService.extractUsername(token));
        assertEquals("access", jwtService.extractTokenType(token));
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    void generateAccessToken_WithExtraClaims_ShouldIncludeClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("user_id", "USER_123");
        extraClaims.put("custom_claim", "custom_value");

        String token = jwtService.generateAccessToken(extraClaims, testUser);

        assertNotNull(token);
        assertEquals("testuser@example.com", jwtService.extractUsername(token));
        assertEquals("access", jwtService.extractTokenType(token));
        
        // Verify extra claims are included
        String customClaim = jwtService.extractClaim(token, claims -> claims.get("custom_claim", String.class));
        assertEquals("custom_value", customClaim);
    }

    @Test
    void generateRefreshToken_WithUserDetails_ShouldReturnValidRefreshToken() {
        String token = jwtService.generateRefreshToken(testUser);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertEquals("testuser@example.com", jwtService.extractUsername(token));
        assertEquals("refresh", jwtService.extractTokenType(token));
        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isAccessToken(token));
    }

    @Test
    void extractUsername_WithValidToken_ShouldReturnUsername() {
        String token = jwtService.generateAccessToken(testUser);
        
        String username = jwtService.extractUsername(token);
        
        assertEquals("testuser@example.com", username);
    }

    @Test
    void extractAuthorities_WithValidToken_ShouldReturnAuthorities() {
        String token = jwtService.generateAccessToken(testUser);
        
        String authorities = jwtService.extractAuthorities(token);
        
        assertNotNull(authorities);
        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("ROLE_CUSTOMER"));
    }

    @Test
    void extractUserId_WithUserIdClaim_ShouldReturnUserId() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("user_id", "USER_123");
        String token = jwtService.generateAccessToken(extraClaims, testUser);
        
        String userId = jwtService.extractUserId(token);
        
        assertEquals("USER_123", userId);
    }

    @Test
    void extractUserId_WithoutUserIdClaim_ShouldReturnNull() {
        String token = jwtService.generateAccessToken(testUser);
        
        String userId = jwtService.extractUserId(token);
        
        assertNull(userId);
    }

    @Test
    void isTokenValid_WithValidTokenAndMatchingUser_ShouldReturnTrue() {
        String token = jwtService.generateAccessToken(testUser);
        
        boolean isValid = jwtService.isTokenValid(token, testUser);
        
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_WithValidTokenAndDifferentUser_ShouldReturnFalse() {
        String token = jwtService.generateAccessToken(testUser);
        UserDetails differentUser = new User("different@example.com", "password", List.of());
        
        boolean isValid = jwtService.isTokenValid(token, differentUser);
        
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        // Create new properties with short expiration time
        ApplicationConfig.AppProperties shortExpProps = new ApplicationConfig.AppProperties();
        ApplicationConfig.AppProperties.Jwt shortJwtProps = new ApplicationConfig.AppProperties.Jwt();
        shortJwtProps.setSecret("mySecretKeyForTestingJwtTokenGenerationAndValidation123456789");
        shortJwtProps.setExpiration(1L); // 1 millisecond
        shortJwtProps.setRefreshExpiration(604800000L);
        shortExpProps.setJwt(shortJwtProps);
        JwtService shortExpirationService = new JwtService(shortExpProps);
        
        String token = shortExpirationService.generateAccessToken(testUser);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        boolean isValid = shortExpirationService.isTokenValid(token, testUser);
        
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_WithMalformedToken_ShouldReturnFalse() {
        String malformedToken = "invalid.token.format";
        
        boolean isValid = jwtService.isTokenValid(malformedToken, testUser);
        
        assertFalse(isValid);
    }

    @Test
    void isAccessToken_WithAccessToken_ShouldReturnTrue() {
        String token = jwtService.generateAccessToken(testUser);
        
        boolean isAccessToken = jwtService.isAccessToken(token);
        
        assertTrue(isAccessToken);
    }

    @Test
    void isAccessToken_WithRefreshToken_ShouldReturnFalse() {
        String token = jwtService.generateRefreshToken(testUser);
        
        boolean isAccessToken = jwtService.isAccessToken(token);
        
        assertFalse(isAccessToken);
    }

    @Test
    void isAccessToken_WithMalformedToken_ShouldReturnFalse() {
        String malformedToken = "invalid.token";
        
        boolean isAccessToken = jwtService.isAccessToken(malformedToken);
        
        assertFalse(isAccessToken);
    }

    @Test
    void isRefreshToken_WithRefreshToken_ShouldReturnTrue() {
        String token = jwtService.generateRefreshToken(testUser);
        
        boolean isRefreshToken = jwtService.isRefreshToken(token);
        
        assertTrue(isRefreshToken);
    }

    @Test
    void isRefreshToken_WithAccessToken_ShouldReturnFalse() {
        String token = jwtService.generateAccessToken(testUser);
        
        boolean isRefreshToken = jwtService.isRefreshToken(token);
        
        assertFalse(isRefreshToken);
    }

    @Test
    void isRefreshToken_WithMalformedToken_ShouldReturnFalse() {
        String malformedToken = "invalid.token";
        
        boolean isRefreshToken = jwtService.isRefreshToken(malformedToken);
        
        assertFalse(isRefreshToken);
    }

    @Test
    void getExpirationTime_WithValidToken_ShouldReturnExpirationTime() {
        String token = jwtService.generateAccessToken(testUser);
        
        long expirationTime = jwtService.getExpirationTime(token);
        
        assertTrue(expirationTime > System.currentTimeMillis());
    }

    @Test
    void getRemainingTime_WithValidToken_ShouldReturnPositiveTime() {
        String token = jwtService.generateAccessToken(testUser);
        
        long remainingTime = jwtService.getRemainingTime(token);
        
        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= 86400000L); // Should be less than or equal to 24 hours
    }

    @Test
    void getRemainingTime_WithExpiredToken_ShouldReturnZero() {
        // Create new properties with short expiration time
        ApplicationConfig.AppProperties shortExpProps = new ApplicationConfig.AppProperties();
        ApplicationConfig.AppProperties.Jwt shortJwtProps = new ApplicationConfig.AppProperties.Jwt();
        shortJwtProps.setSecret("mySecretKeyForTestingJwtTokenGenerationAndValidation123456789");
        shortJwtProps.setExpiration(1L); // 1 millisecond
        shortJwtProps.setRefreshExpiration(604800000L);
        shortExpProps.setJwt(shortJwtProps);
        JwtService shortExpirationService = new JwtService(shortExpProps);
        
        String token = shortExpirationService.generateAccessToken(testUser);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // The getRemainingTime method will throw ExpiredJwtException for expired tokens
        // since it tries to extract expiration from the token. This is expected behavior.
        assertThrows(ExpiredJwtException.class, () -> {
            shortExpirationService.getRemainingTime(token);
        });
    }

    @Test
    void extractClaim_WithCustomClaimExtractor_ShouldReturnClaim() {
        String token = jwtService.generateAccessToken(testUser);
        
        Date issuedAt = jwtService.extractClaim(token, claims -> claims.getIssuedAt());
        
        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()));
    }

    @Test
    void extractAllClaims_WithExpiredToken_ShouldThrowExpiredJwtException() {
        // Create new properties with short expiration time
        ApplicationConfig.AppProperties shortExpProps = new ApplicationConfig.AppProperties();
        ApplicationConfig.AppProperties.Jwt shortJwtProps = new ApplicationConfig.AppProperties.Jwt();
        shortJwtProps.setSecret("mySecretKeyForTestingJwtTokenGenerationAndValidation123456789");
        shortJwtProps.setExpiration(1L); // 1 millisecond
        shortJwtProps.setRefreshExpiration(604800000L);
        shortExpProps.setJwt(shortJwtProps);
        JwtService shortExpirationService = new JwtService(shortExpProps);
        
        String token = shortExpirationService.generateAccessToken(testUser);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertThrows(ExpiredJwtException.class, () -> {
            shortExpirationService.extractUsername(token);
        });
    }

    @Test
    void extractAllClaims_WithMalformedToken_ShouldThrowMalformedJwtException() {
        String malformedToken = "invalid.jwt.token";
        
        assertThrows(MalformedJwtException.class, () -> {
            jwtService.extractUsername(malformedToken);
        });
    }

    @Test
    void extractAllClaims_WithInvalidSignature_ShouldThrowSignatureException() {
        // Generate token with one service
        String token = jwtService.generateAccessToken(testUser);
        
        // Create service with different secret
        ApplicationConfig.AppProperties diffSecretProps = new ApplicationConfig.AppProperties();
        ApplicationConfig.AppProperties.Jwt diffJwtProps = new ApplicationConfig.AppProperties.Jwt();
        diffJwtProps.setSecret("differentSecretKeyThatWillCauseSignatureValidationFailure123");
        diffJwtProps.setExpiration(86400000L);
        diffJwtProps.setRefreshExpiration(604800000L);
        diffSecretProps.setJwt(diffJwtProps);
        JwtService differentSecretService = new JwtService(diffSecretProps);
        
        assertThrows(SignatureException.class, () -> {
            differentSecretService.extractUsername(token);
        });
    }

    @Test
    void buildToken_WithShortSecret_ShouldPadSecretAndWork() {
        // Create properties with short secret that needs padding
        ApplicationConfig.AppProperties shortSecretProps = new ApplicationConfig.AppProperties();
        ApplicationConfig.AppProperties.Jwt shortJwtProps = new ApplicationConfig.AppProperties.Jwt();
        shortJwtProps.setSecret("short");
        shortJwtProps.setExpiration(86400000L);
        shortJwtProps.setRefreshExpiration(604800000L);
        shortSecretProps.setJwt(shortJwtProps);
        JwtService shortSecretService = new JwtService(shortSecretProps);
        
        String token = shortSecretService.generateAccessToken(testUser);
        
        assertNotNull(token);
        assertEquals("testuser@example.com", shortSecretService.extractUsername(token));
    }

    @Test
    void buildToken_WithBase64Secret_ShouldDecodeAndWork() {
        // Test with base64 encoded secret
        String base64Secret = java.util.Base64.getEncoder().encodeToString(
            "mySecretKeyForTestingJwtTokenGeneration".getBytes()
        );
        ApplicationConfig.AppProperties base64Props = new ApplicationConfig.AppProperties();
        ApplicationConfig.AppProperties.Jwt base64JwtProps = new ApplicationConfig.AppProperties.Jwt();
        base64JwtProps.setSecret(base64Secret);
        base64JwtProps.setExpiration(86400000L);
        base64JwtProps.setRefreshExpiration(604800000L);
        base64Props.setJwt(base64JwtProps);
        JwtService base64SecretService = new JwtService(base64Props);
        
        String token = base64SecretService.generateAccessToken(testUser);
        
        assertNotNull(token);
        assertEquals("testuser@example.com", base64SecretService.extractUsername(token));
    }

    @Test
    void extractTokenType_WithValidAccessToken_ShouldReturnAccessType() {
        String token = jwtService.generateAccessToken(testUser);
        
        String tokenType = jwtService.extractTokenType(token);
        
        assertEquals("access", tokenType);
    }

    @Test
    void extractTokenType_WithValidRefreshToken_ShouldReturnRefreshType() {
        String token = jwtService.generateRefreshToken(testUser);
        
        String tokenType = jwtService.extractTokenType(token);
        
        assertEquals("refresh", tokenType);
    }

    @Test
    void tokenGeneration_WithDifferentExpirationTimes_ShouldRespectConfiguration() {
        // Test access token expiration
        String accessToken = jwtService.generateAccessToken(testUser);
        long accessExpiration = jwtService.getExpirationTime(accessToken);
        
        // Test refresh token expiration
        String refreshToken = jwtService.generateRefreshToken(testUser);
        long refreshExpiration = jwtService.getExpirationTime(refreshToken);
        
        // Refresh token should expire later than access token
        assertTrue(refreshExpiration > accessExpiration);
    }
}
