package com.talentica.paymentgateway.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthenticationRequest DTO.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
class AuthenticationRequestUnitTest {

    private Validator validator;
    private AuthenticationRequest authenticationRequest;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        authenticationRequest = new AuthenticationRequest();
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyRequest() {
        // When
        AuthenticationRequest request = new AuthenticationRequest();

        // Then
        assertNotNull(request);
        assertNull(request.getEmail());
        assertNull(request.getPassword());
    }

    @Test
    void constructorWithParameters_ShouldSetFields() {
        // Given
        String email = "test@example.com";
        String password = "password123";

        // When
        AuthenticationRequest request = new AuthenticationRequest(email, password);

        // Then
        assertEquals(email, request.getEmail());
        assertEquals(password, request.getPassword());
    }

    @Test
    void setEmail_WithValidEmail_ShouldSetEmail() {
        // Given
        String email = "user@domain.com";

        // When
        authenticationRequest.setEmail(email);

        // Then
        assertEquals(email, authenticationRequest.getEmail());
    }

    @Test
    void setPassword_WithValidPassword_ShouldSetPassword() {
        // Given
        String password = "securePassword123";

        // When
        authenticationRequest.setPassword(password);

        // Then
        assertEquals(password, authenticationRequest.getPassword());
    }

    @Test
    void validation_WithValidData_ShouldHaveNoViolations() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("password123");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_WithNullEmail_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail(null);
        authenticationRequest.setPassword("password123");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Email is required")));
    }

    @Test
    void validation_WithBlankEmail_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("");
        authenticationRequest.setPassword("password123");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Email is required")));
    }

    @Test
    void validation_WithWhitespaceEmail_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("   ");
        authenticationRequest.setPassword("password123");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Email is required")));
    }

    @Test
    void validation_WithInvalidEmailFormat_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("invalid-email");
        authenticationRequest.setPassword("password123");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Invalid email format")));
    }

    @Test
    void validation_WithEmailMissingAtSymbol_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("testexample.com");
        authenticationRequest.setPassword("password123");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Invalid email format")));
    }

    @Test
    void validation_WithEmailMissingDomain_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("test@");
        authenticationRequest.setPassword("password123");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Invalid email format")));
    }

    @Test
    void validation_WithNullPassword_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword(null);

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password is required")));
    }

    @Test
    void validation_WithBlankPassword_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password is required")));
    }

    @Test
    void validation_WithWhitespacePassword_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("   ");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password is required")));
    }

    @Test
    void validation_WithTooShortPassword_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("1234567"); // 7 characters, minimum is 8

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password must be between 8 and 128 characters")));
    }

    @Test
    void validation_WithTooLongPassword_ShouldHaveViolation() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("a".repeat(129)); // 129 characters, maximum is 128

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password must be between 8 and 128 characters")));
    }

    @Test
    void validation_WithMinimumLengthPassword_ShouldHaveNoViolations() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("12345678"); // Exactly 8 characters

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_WithMaximumLengthPassword_ShouldHaveNoViolations() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("a".repeat(128)); // Exactly 128 characters

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_WithValidEmailFormats_ShouldHaveNoViolations() {
        // Given
        String[] validEmails = {
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org",
            "123@numbers.com",
            "test_user@example-domain.com"
        };

        for (String email : validEmails) {
            AuthenticationRequest request = new AuthenticationRequest();
            request.setEmail(email);
            request.setPassword("password123");

            // When
            Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

            // Then
            assertTrue(violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("email")),
                "Email " + email + " should be valid");
        }
    }

    @Test
    void validation_WithInvalidEmailFormats_ShouldHaveViolations() {
        // Given
        String[] invalidEmails = {
            "plainaddress",
            "@missingusername.com",
            "username@.com",
            "username@com",
            "username..double.dot@example.com"
        };

        for (String email : invalidEmails) {
            AuthenticationRequest request = new AuthenticationRequest();
            request.setEmail(email);
            request.setPassword("password123");

            // When
            Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(request);

            // Then
            assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email")),
                "Email " + email + " should be invalid");
        }
    }

    @Test
    void validation_WithBothInvalidEmailAndPassword_ShouldHaveMultipleViolations() {
        // Given
        authenticationRequest.setEmail("invalid-email");
        authenticationRequest.setPassword("short"); // Too short

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertEquals(2, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Invalid email format")));
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password must be between 8 and 128 characters")));
    }

    @Test
    void setEmail_WithNullEmail_ShouldSetNull() {
        // When
        authenticationRequest.setEmail(null);

        // Then
        assertNull(authenticationRequest.getEmail());
    }

    @Test
    void setPassword_WithNullPassword_ShouldSetNull() {
        // When
        authenticationRequest.setPassword(null);

        // Then
        assertNull(authenticationRequest.getPassword());
    }

    @Test
    void validation_WithSpecialCharactersInPassword_ShouldHaveNoViolations() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("P@ssw0rd!#$%");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_WithUnicodeCharactersInPassword_ShouldHaveNoViolations() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("pässwörd123");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_WithNumericPassword_ShouldHaveNoViolations() {
        // Given
        authenticationRequest.setEmail("test@example.com");
        authenticationRequest.setPassword("12345678");

        // When
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(authenticationRequest);

        // Then
        assertTrue(violations.isEmpty());
    }
}
