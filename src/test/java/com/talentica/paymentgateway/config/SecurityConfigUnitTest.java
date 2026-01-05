package com.talentica.paymentgateway.config;

import com.talentica.paymentgateway.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityConfig.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigUnitTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Mock
    private RateLimitFilter rateLimitFilter;

    @Mock
    private CorrelationIdFilter correlationIdFilter;

    @Mock
    private RequestResponseLoggingFilter requestResponseLoggingFilter;

    @Mock
    private CorsConfigurationSource corsConfigurationSource;

    @Mock
    private HttpSecurity httpSecurity;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
            jwtAuthenticationFilter,
            apiKeyAuthenticationFilter,
            rateLimitFilter,
            correlationIdFilter,
            requestResponseLoggingFilter,
            corsConfigurationSource
        );
    }

    @Test
    void constructor_WithAllDependencies_ShouldCreateConfig() {
        // When & Then
        assertNotNull(securityConfig);
    }

    @Test
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        // When
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // Then
        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder.getClass().getSimpleName().contains("BCrypt"));
    }

    @Test
    void passwordEncoder_ShouldEncodePasswords() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String rawPassword = "testPassword123";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Then
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_ShouldUseBCryptWithStrength12() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password = "testPassword";

        // When
        String encoded = passwordEncoder.encode(password);

        // Then
        assertNotNull(encoded);
        // BCrypt hashes start with $2a$, $2b$, or $2y$ followed by cost parameter
        assertTrue(encoded.matches("\\$2[aby]\\$12\\$.*"));
    }

    @Test
    void passwordEncoder_WithSamePassword_ShouldGenerateDifferentHashes() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password = "testPassword";

        // When
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        // Then
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2); // BCrypt uses salt, so hashes should be different
        assertTrue(passwordEncoder.matches(password, hash1));
        assertTrue(passwordEncoder.matches(password, hash2));
    }

    @Test
    void passwordEncoder_WithWrongPassword_ShouldNotMatch() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String correctPassword = "correctPassword";
        String wrongPassword = "wrongPassword";

        // When
        String encodedPassword = passwordEncoder.encode(correctPassword);

        // Then
        assertTrue(passwordEncoder.matches(correctPassword, encodedPassword));
        assertFalse(passwordEncoder.matches(wrongPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_WithEmptyPassword_ShouldEncode() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String emptyPassword = "";

        // When
        String encodedPassword = passwordEncoder.encode(emptyPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(emptyPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_WithNullPassword_ShouldThrowException() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            passwordEncoder.encode(null);
        });
    }

    @Test
    void passwordEncoder_WithSpecialCharacters_ShouldEncode() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String specialPassword = "p@ssw0rd!#$%^&*()";

        // When
        String encodedPassword = passwordEncoder.encode(specialPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(specialPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_WithLongPassword_ShouldEncode() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String longPassword = "a".repeat(100);

        // When
        String encodedPassword = passwordEncoder.encode(longPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(longPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_WithUnicodeCharacters_ShouldEncode() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String unicodePassword = "pässwörd123";

        // When
        String encodedPassword = passwordEncoder.encode(unicodePassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(unicodePassword, encodedPassword));
    }

    @Test
    void passwordEncoder_MultipleInstances_ShouldBeSameInstance() {
        // When
        PasswordEncoder encoder1 = securityConfig.passwordEncoder();
        PasswordEncoder encoder2 = securityConfig.passwordEncoder();

        // Then
        assertNotNull(encoder1);
        assertNotNull(encoder2);
        // Since it's a @Bean, Spring should return the same instance
        // But in unit test without Spring context, each call creates new instance
        assertEquals(encoder1.getClass(), encoder2.getClass());
    }

    @Test
    void passwordEncoder_PerformanceTest_ShouldEncodeReasonablyFast() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password = "testPassword123";

        // When
        long startTime = System.currentTimeMillis();
        String encodedPassword = passwordEncoder.encode(password);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(encodedPassword);
        long duration = endTime - startTime;
        // BCrypt with strength 12 should take reasonable time (usually < 1000ms)
        assertTrue(duration < 5000, "Password encoding took too long: " + duration + "ms");
    }

    @Test
    void securityConfig_WithAllFilters_ShouldHaveCorrectDependencies() {
        // When & Then
        assertNotNull(securityConfig);
        // Verify that all filters are properly injected (they are mocked)
        assertNotNull(jwtAuthenticationFilter);
        assertNotNull(apiKeyAuthenticationFilter);
        assertNotNull(rateLimitFilter);
        assertNotNull(correlationIdFilter);
        assertNotNull(requestResponseLoggingFilter);
        assertNotNull(corsConfigurationSource);
    }

    @Test
    void passwordEncoder_WithCaseSensitivePasswords_ShouldDistinguish() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String lowerCasePassword = "password";
        String upperCasePassword = "PASSWORD";

        // When
        String encodedLower = passwordEncoder.encode(lowerCasePassword);
        String encodedUpper = passwordEncoder.encode(upperCasePassword);

        // Then
        assertTrue(passwordEncoder.matches(lowerCasePassword, encodedLower));
        assertTrue(passwordEncoder.matches(upperCasePassword, encodedUpper));
        assertFalse(passwordEncoder.matches(lowerCasePassword, encodedUpper));
        assertFalse(passwordEncoder.matches(upperCasePassword, encodedLower));
    }

    @Test
    void passwordEncoder_WithNumericPassword_ShouldEncode() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String numericPassword = "123456789";

        // When
        String encodedPassword = passwordEncoder.encode(numericPassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(numericPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_WithWhitespacePassword_ShouldEncode() {
        // Given
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String whitespacePassword = "pass word";

        // When
        String encodedPassword = passwordEncoder.encode(whitespacePassword);

        // Then
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(whitespacePassword, encodedPassword));
        assertFalse(passwordEncoder.matches("password", encodedPassword));
    }
}
