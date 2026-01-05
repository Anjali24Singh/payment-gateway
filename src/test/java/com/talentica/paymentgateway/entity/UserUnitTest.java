package com.talentica.paymentgateway.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for User entity.
 * Tests constructors, getters, setters, validation constraints, relationships, and utility methods.
 */
@DisplayName("User Entity Unit Tests")
class UserUnitTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create User with default constructor")
    void shouldCreateWithDefaultConstructor() {
        // When
        User user = new User();

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
        assertThat(user.getIsActive()).isTrue();
        assertThat(user.getIsVerified()).isFalse();
        assertThat(user.getLastLoginAt()).isNull();
        assertThat(user.getApiKeys()).isNotNull().isEmpty();
        assertThat(user.getCustomers()).isNotNull().isEmpty();
        assertThat(user.getOrders()).isNotNull().isEmpty();
        assertThat(user.getAuditLogs()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should create User with parameterized constructor")
    void shouldCreateWithParameterizedConstructor() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        String passwordHash = "hashedPassword123";

        // When
        User user = new User(username, email, passwordHash);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(user.getIsActive()).isTrue();
        assertThat(user.getIsVerified()).isFalse();
        assertThat(user.getApiKeys()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void shouldSetAndGetAllFields() {
        // Given
        User user = new User();
        String username = "newuser";
        String email = "new@example.com";
        String passwordHash = "newHashedPassword";
        String firstName = "John";
        String lastName = "Doe";
        ZonedDateTime lastLogin = ZonedDateTime.now();

        // When
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setIsActive(false);
        user.setIsVerified(true);
        user.setLastLoginAt(lastLogin);

        // Then
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(user.getFirstName()).isEqualTo(firstName);
        assertThat(user.getLastName()).isEqualTo(lastName);
        assertThat(user.getIsActive()).isFalse();
        assertThat(user.getIsVerified()).isTrue();
        assertThat(user.getLastLoginAt()).isEqualTo(lastLogin);
    }

    @Test
    @DisplayName("Should validate successfully with valid data")
    void shouldValidateSuccessfullyWithValidData() {
        // Given
        User user = new User();
        user.setUsername("validuser");
        user.setEmail("valid@example.com");
        user.setPasswordHash("validHashedPassword");
        user.setFirstName("John");
        user.setLastName("Doe");

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when username is null")
    void shouldFailValidationWhenUsernameIsNull() {
        // Given
        User user = new User();
        user.setUsername(null);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Username is required");
    }

    @Test
    @DisplayName("Should fail validation when username is blank")
    void shouldFailValidationWhenUsernameIsBlank() {
        // Given
        User user = new User();
        user.setUsername("");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Username is required");
    }

    @Test
    @DisplayName("Should fail validation when username exceeds max length")
    void shouldFailValidationWhenUsernameExceedsMaxLength() {
        // Given
        User user = new User();
        user.setUsername("a".repeat(101));
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Username must not exceed 100 characters");
    }

    @Test
    @DisplayName("Should fail validation when email is null")
    void shouldFailValidationWhenEmailIsNull() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail(null);
        user.setPasswordHash("hashedPassword");

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "test@", "@example.com", "test.example.com"})
    @DisplayName("Should fail validation with invalid email formats")
    void shouldFailValidationWithInvalidEmailFormats(String invalidEmail) {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail(invalidEmail);
        user.setPasswordHash("hashedPassword");

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email must be valid");
    }

    @Test
    @DisplayName("Should fail validation when email exceeds max length")
    void shouldFailValidationWhenEmailExceedsMaxLength() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("a".repeat(250) + "@example.com");
        user.setPasswordHash("hashedPassword");

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then - Expects 2 violations: @Size and @Email
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
            .contains("Email must not exceed 255 characters");
    }

    @Test
    @DisplayName("Should fail validation when password hash is null")
    void shouldFailValidationWhenPasswordHashIsNull() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash(null);

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Password is required");
    }

    @Test
    @DisplayName("Should fail validation when first name exceeds max length")
    void shouldFailValidationWhenFirstNameExceedsMaxLength() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setFirstName("a".repeat(101));

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("First name must not exceed 100 characters");
    }

    @Test
    @DisplayName("Should fail validation when last name exceeds max length")
    void shouldFailValidationWhenLastNameExceedsMaxLength() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setLastName("a".repeat(101));

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Last name must not exceed 100 characters");
    }

    @Test
    @DisplayName("Should return full name when both first and last names are present")
    void shouldReturnFullNameWhenBothNamesPresent() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setFirstName("John");
        user.setLastName("Doe");

        // When
        String fullName = user.getFullName();

        // Then
        assertThat(fullName).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return first name when only first name is present")
    void shouldReturnFirstNameWhenOnlyFirstNamePresent() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setFirstName("John");
        user.setLastName(null);

        // When
        String fullName = user.getFullName();

        // Then
        assertThat(fullName).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return last name when only last name is present")
    void shouldReturnLastNameWhenOnlyLastNamePresent() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setFirstName(null);
        user.setLastName("Doe");

        // When
        String fullName = user.getFullName();

        // Then
        assertThat(fullName).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should return username when no names are present")
    void shouldReturnUsernameWhenNoNamesPresent() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setFirstName(null);
        user.setLastName(null);

        // When
        String fullName = user.getFullName();

        // Then
        assertThat(fullName).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should add API key correctly")
    void shouldAddApiKeyCorrectly() {
        // Given
        User user = new User();
        ApiKey apiKey = new ApiKey();

        // When
        user.addApiKey(apiKey);

        // Then
        assertThat(user.getApiKeys()).contains(apiKey);
        assertThat(apiKey.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should remove API key correctly")
    void shouldRemoveApiKeyCorrectly() {
        // Given
        User user = new User();
        ApiKey apiKey = new ApiKey();
        user.addApiKey(apiKey);

        // When
        user.removeApiKey(apiKey);

        // Then
        assertThat(user.getApiKeys()).doesNotContain(apiKey);
        assertThat(apiKey.getUser()).isNull();
    }

    @Test
    @DisplayName("Should handle multiple API keys")
    void shouldHandleMultipleApiKeys() {
        // Given
        User user = new User();
        ApiKey apiKey1 = new ApiKey();
        ApiKey apiKey2 = new ApiKey();

        // When
        user.addApiKey(apiKey1);
        user.addApiKey(apiKey2);

        // Then
        assertThat(user.getApiKeys()).hasSize(2);
        assertThat(user.getApiKeys()).contains(apiKey1, apiKey2);
        assertThat(apiKey1.getUser()).isEqualTo(user);
        assertThat(apiKey2.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should set and get relationship collections")
    void shouldSetAndGetRelationshipCollections() {
        // Given
        User user = new User();
        List<ApiKey> apiKeys = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<Order> orders = new ArrayList<>();
        List<AuditLog> auditLogs = new ArrayList<>();

        // When
        user.setApiKeys(apiKeys);
        user.setCustomers(customers);
        user.setOrders(orders);
        user.setAuditLogs(auditLogs);

        // Then
        assertThat(user.getApiKeys()).isEqualTo(apiKeys);
        assertThat(user.getCustomers()).isEqualTo(customers);
        assertThat(user.getOrders()).isEqualTo(orders);
        assertThat(user.getAuditLogs()).isEqualTo(auditLogs);
    }

    @Test
    @DisplayName("Should handle null relationship collections")
    void shouldHandleNullRelationshipCollections() {
        // Given
        User user = new User();

        // When
        user.setApiKeys(null);
        user.setCustomers(null);
        user.setOrders(null);
        user.setAuditLogs(null);

        // Then
        assertThat(user.getApiKeys()).isNull();
        assertThat(user.getCustomers()).isNull();
        assertThat(user.getOrders()).isNull();
        assertThat(user.getAuditLogs()).isNull();
    }

    @Test
    @DisplayName("Should handle boolean flags correctly")
    void shouldHandleBooleanFlagsCorrectly() {
        // Given
        User user = new User();

        // When & Then - Test default values
        assertThat(user.getIsActive()).isTrue();
        assertThat(user.getIsVerified()).isFalse();

        // When - Change values
        user.setIsActive(false);
        user.setIsVerified(true);

        // Then
        assertThat(user.getIsActive()).isFalse();
        assertThat(user.getIsVerified()).isTrue();
    }

    @Test
    @DisplayName("Should handle last login timestamp")
    void shouldHandleLastLoginTimestamp() {
        // Given
        User user = new User();
        ZonedDateTime loginTime = ZonedDateTime.now();

        // When
        user.setLastLoginAt(loginTime);

        // Then
        assertThat(user.getLastLoginAt()).isEqualTo(loginTime);
    }

    @Test
    @DisplayName("Should validate with maximum length fields")
    void shouldValidateWithMaximumLengthFields() {
        // Given
        User user = new User();
        user.setUsername("a".repeat(100));
        user.setEmail("valid.test.email.for.maximum.length.validation@example.com"); // Simple valid email
        user.setPasswordHash("a".repeat(255));
        user.setFirstName("a".repeat(100));
        user.setLastName("a".repeat(100));

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty string names in getFullName")
    void shouldHandleEmptyStringNamesInGetFullName() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setFirstName("");
        user.setLastName("");

        // When
        String fullName = user.getFullName();

        // Then
        assertThat(fullName).isEqualTo("testuser");
    }
}
