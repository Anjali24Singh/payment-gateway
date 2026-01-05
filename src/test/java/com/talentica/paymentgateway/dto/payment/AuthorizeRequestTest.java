package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizeRequestTest {

    private AuthorizeRequest authorizeRequest;
    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authorizeRequest = new AuthorizeRequest();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should create AuthorizeRequest with default values")
    void defaultConstructor() {
        assertThat(authorizeRequest.getHoldPeriodDays()).isNull();
        assertThat(authorizeRequest.getAutoCapture()).isFalse();
    }

    @Test
    @DisplayName("Should set and get hold period days")
    void holdPeriodDaysProperty() {
        authorizeRequest.setHoldPeriodDays(7);
        assertThat(authorizeRequest.getHoldPeriodDays()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should set and get auto capture flag")
    void autoCaptureProperty() {
        authorizeRequest.setAutoCapture(true);
        assertThat(authorizeRequest.getAutoCapture()).isTrue();
        
        authorizeRequest.setAutoCapture(false);
        assertThat(authorizeRequest.getAutoCapture()).isFalse();
    }

    @Test
    @DisplayName("Should validate hold period days minimum value")
    void validateHoldPeriodDaysMinimum() {
        authorizeRequest.setHoldPeriodDays(0);
        
        Set<ConstraintViolation<AuthorizeRequest>> violations = validator.validate(authorizeRequest);
        
        // Check that the specific validation message exists
        boolean hasMinValidation = violations.stream()
            .anyMatch(v -> v.getMessage().equals("Authorization hold period must be at least 1 day"));
        assertThat(hasMinValidation).isTrue();
    }

    @Test
    @DisplayName("Should validate hold period days maximum value")
    void validateHoldPeriodDaysMaximum() {
        authorizeRequest.setHoldPeriodDays(31);
        
        Set<ConstraintViolation<AuthorizeRequest>> violations = validator.validate(authorizeRequest);
        
        // Check that the specific validation message exists
        boolean hasMaxValidation = violations.stream()
            .anyMatch(v -> v.getMessage().equals("Authorization hold period cannot exceed 30 days"));
        assertThat(hasMaxValidation).isTrue();
    }

    @Test
    @DisplayName("Should pass validation with valid hold period days")
    void validateValidHoldPeriodDays() {
        authorizeRequest.setHoldPeriodDays(7);
        
        Set<ConstraintViolation<AuthorizeRequest>> violations = validator.validate(authorizeRequest);
        
        // Should only have violations from parent class if any, not from holdPeriodDays
        assertThat(violations.stream()
            .noneMatch(v -> v.getMessage().contains("Authorization hold period")))
            .isTrue();
    }

    @Test
    @DisplayName("Should generate correct toString representation")
    void toStringMethod() {
        authorizeRequest.setHoldPeriodDays(7);
        authorizeRequest.setAutoCapture(true);
        
        String result = authorizeRequest.toString();
        
        assertThat(result).contains("AuthorizeRequest("); // Lombok format
        assertThat(result).contains("holdPeriodDays=7");
        assertThat(result).contains("autoCapture=true");
    }

    @Test
    @DisplayName("Should serialize to JSON correctly")
    void jsonSerialization() throws Exception {
        authorizeRequest.setHoldPeriodDays(7);
        authorizeRequest.setAutoCapture(true);
        
        String json = objectMapper.writeValueAsString(authorizeRequest);
        
        assertThat(json).contains("\"holdPeriodDays\":7");
        assertThat(json).contains("\"autoCapture\":true");
    }

    @Test
    @DisplayName("Should deserialize from JSON correctly")
    void jsonDeserialization() throws Exception {
        String json = "{\"holdPeriodDays\":14,\"autoCapture\":false}";
        
        AuthorizeRequest result = objectMapper.readValue(json, AuthorizeRequest.class);
        
        assertThat(result.getHoldPeriodDays()).isEqualTo(14);
        assertThat(result.getAutoCapture()).isFalse();
    }

    @Test
    @DisplayName("Should handle null values correctly")
    void nullValues() {
        authorizeRequest.setHoldPeriodDays(null);
        authorizeRequest.setAutoCapture(null);
        
        assertThat(authorizeRequest.getHoldPeriodDays()).isNull();
        assertThat(authorizeRequest.getAutoCapture()).isNull();
    }
}
