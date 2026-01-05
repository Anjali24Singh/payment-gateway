package com.talentica.paymentgateway.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RegistrationRequest DTO.
 * Tests constructors, getters, setters, validation constraints, and JSON serialization.
 */
@DisplayName("RegistrationRequest Unit Tests")
class RegistrationRequestUnitTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should create RegistrationRequest with default constructor")
    void shouldCreateWithDefaultConstructor() {
        // When
        RegistrationRequest request = new RegistrationRequest();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getEmail()).isNull();
        assertThat(request.getPassword()).isNull();
        assertThat(request.getFirstName()).isNull();
        assertThat(request.getLastName()).isNull();
        assertThat(request.getPhone()).isNull();
    }

    @Test
    @DisplayName("Should create RegistrationRequest with parameterized constructor")
    void shouldCreateWithParameterizedConstructor() {
        // Given
        String email = "test@example.com";
        String password = "SecurePass123!";

        // When
        RegistrationRequest request = RegistrationRequest.builder()
            .email(email)
            .password(password)
            .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getEmail()).isEqualTo(email);
        assertThat(request.getPassword()).isEqualTo(password);
        assertThat(request.getFirstName()).isNull();
        assertThat(request.getLastName()).isNull();
        assertThat(request.getPhone()).isNull();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void shouldSetAndGetAllFields() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        String email = "user@example.com";
        String password = "MyPassword123!";
        String firstName = "John";
        String lastName = "Doe";
        String phone = "+1-555-123-4567";

        // When
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setPhone(phone);

        // Then
        assertThat(request.getEmail()).isEqualTo(email);
        assertThat(request.getPassword()).isEqualTo(password);
        assertThat(request.getFirstName()).isEqualTo(firstName);
        assertThat(request.getLastName()).isEqualTo(lastName);
        assertThat(request.getPhone()).isEqualTo(phone);
    }

    @Test
    @DisplayName("Should validate successfully with valid data")
    void shouldValidateSuccessfullyWithValidData() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("valid@example.com");
        request.setPassword("ValidPass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("+1234567890");

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when email is null")
    void shouldFailValidationWhenEmailIsNull() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(null);
        request.setPassword("ValidPass123!");

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email is required");
    }

    @Test
    @DisplayName("Should fail validation when email is blank")
    void shouldFailValidationWhenEmailIsBlank() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("");
        request.setPassword("ValidPass123!");

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "test@", "@example.com", "test.example.com", "test@.com"})
    @DisplayName("Should fail validation with invalid email formats")
    void shouldFailValidationWithInvalidEmailFormats(String invalidEmail) {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(invalidEmail);
        request.setPassword("ValidPass123!");

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email must be valid");
    }

    @Test
    @DisplayName("Should fail validation when password is null")
    void shouldFailValidationWhenPasswordIsNull() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword(null);

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Password is required");
    }

    @Test
    @DisplayName("Should fail validation when password is too short")
    void shouldFailValidationWhenPasswordIsTooShort() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("Short1!");

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then - Expects 2 violations: @Size and @Pattern
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
            .containsExactlyInAnyOrder(
                "Password must be between 8 and 50 characters",
                "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
            );
    }

    @Test
    @DisplayName("Should fail validation when password is too long")
    void shouldFailValidationWhenPasswordIsTooLong() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("A".repeat(51) + "1!");

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then - Expects 2 violations: @Size and @Pattern
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
            .containsExactlyInAnyOrder(
                "Password must be between 8 and 50 characters",
                "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
            );
    }

    @ParameterizedTest
    @ValueSource(strings = {"password123", "PASSWORD123", "Password", "Password123", "password!"})
    @DisplayName("Should fail validation with weak passwords")
    void shouldFailValidationWithWeakPasswords(String weakPassword) {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword(weakPassword);

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
    }

    @Test
    @DisplayName("Should fail validation when first name is too long")
    void shouldFailValidationWhenFirstNameIsTooLong() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("ValidPass123!");
        request.setFirstName("A".repeat(51));

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("First name must not exceed 50 characters");
    }

    @Test
    @DisplayName("Should fail validation when last name is too long")
    void shouldFailValidationWhenLastNameIsTooLong() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("ValidPass123!");
        request.setLastName("A".repeat(51));

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Last name must not exceed 50 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abc", "123-456", "+", "+1", "++1234567890", "123456789012345678", "0123456789"})
    @DisplayName("Should fail validation with invalid phone numbers")
    void shouldFailValidationWithInvalidPhoneNumbers(String invalidPhone) {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("ValidPass123!");
        request.setPhone(invalidPhone);

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then - Phone pattern: ^\\+?[1-9]\\d{1,14}$ should reject these
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Phone number must be valid"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+1234567890", "+12345678901234", "1234567890", "+44123456789"})
    @DisplayName("Should validate successfully with valid phone numbers")
    void shouldValidateSuccessfullyWithValidPhoneNumbers(String validPhone) {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("ValidPass123!");
        request.setPhone(validPhone);

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should serialize to JSON correctly")
    void shouldSerializeToJsonCorrectly() throws Exception {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("+1234567890");

        // When
        String json = objectMapper.writeValueAsString(request);

        // Then
        assertThat(json).contains("\"email\":\"test@example.com\"");
        assertThat(json).contains("\"password\":\"SecurePass123!\"");
        assertThat(json).contains("\"firstName\":\"John\"");
        assertThat(json).contains("\"lastName\":\"Doe\"");
        assertThat(json).contains("\"phone\":\"+1234567890\"");
    }

    @Test
    @DisplayName("Should deserialize from JSON correctly")
    void shouldDeserializeFromJsonCorrectly() throws Exception {
        // Given
        String json = """
            {
                "email": "test@example.com",
                "password": "SecurePass123!",
                "firstName": "John",
                "lastName": "Doe",
                "phone": "+1234567890"
            }
            """;

        // When
        RegistrationRequest request = objectMapper.readValue(json, RegistrationRequest.class);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getEmail()).isEqualTo("test@example.com");
        assertThat(request.getPassword()).isEqualTo("SecurePass123!");
        assertThat(request.getFirstName()).isEqualTo("John");
        assertThat(request.getLastName()).isEqualTo("Doe");
        assertThat(request.getPhone()).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("ValidPass123!");
        request.setFirstName(null);
        request.setLastName(null);
        request.setPhone(null);

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
        assertThat(request.getFirstName()).isNull();
        assertThat(request.getLastName()).isNull();
        assertThat(request.getPhone()).isNull();
    }

    @Test
    @DisplayName("Should generate toString without password")
    void shouldGenerateToStringWithoutPassword() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("+1234567890");

        // When
        String toString = request.toString();

        // Then - Lombok format without quotes
        assertThat(toString).contains("email=test@example.com");
        assertThat(toString).contains("firstName=John");
        assertThat(toString).contains("lastName=Doe");
        assertThat(toString).contains("phone=+1234567890");
        assertThat(toString).doesNotContain("password");
        assertThat(toString).doesNotContain("SecurePass123!");
    }

    @Test
    @DisplayName("Should validate with minimum valid password")
    void shouldValidateWithMinimumValidPassword() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("MinPass1!");

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should validate with maximum length names")
    void shouldValidateWithMaximumLengthNames() {
        // Given
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("ValidPass123!");
        request.setFirstName("A".repeat(50));
        request.setLastName("B".repeat(50));

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }
}
