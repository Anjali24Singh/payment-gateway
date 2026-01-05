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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Customer entity.
 * Tests constructors, getters, setters, validation constraints, relationships, and utility methods.
 */
@DisplayName("Customer Entity Unit Tests")
class CustomerUnitTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create Customer with default constructor")
    void shouldCreateWithDefaultConstructor() {
        // When
        Customer customer = new Customer();

        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getUser()).isNull();
        assertThat(customer.getCustomerReference()).isNull();
        assertThat(customer.getEmail()).isNull();
        assertThat(customer.getFirstName()).isNull();
        assertThat(customer.getLastName()).isNull();
        assertThat(customer.getPhone()).isNull();
        assertThat(customer.getCompany()).isNull();
        assertThat(customer.getBillingCountry()).isEqualTo("US");
        assertThat(customer.getShippingCountry()).isEqualTo("US");
        assertThat(customer.getIsActive()).isTrue();
        assertThat(customer.getAuthorizeNetCustomerProfileId()).isNull();
        assertThat(customer.getPaymentMethods()).isNotNull().isEmpty();
        assertThat(customer.getOrders()).isNotNull().isEmpty();
        assertThat(customer.getTransactions()).isNotNull().isEmpty();
        assertThat(customer.getSubscriptions()).isNotNull().isEmpty();
        assertThat(customer.getSubscriptionInvoices()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should create Customer with email constructor")
    void shouldCreateWithEmailConstructor() {
        // Given
        String email = "test@example.com";

        // When
        Customer customer = new Customer(email);

        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getEmail()).isEqualTo(email);
        assertThat(customer.getFirstName()).isNull();
        assertThat(customer.getLastName()).isNull();
        assertThat(customer.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should create Customer with full name constructor")
    void shouldCreateWithFullNameConstructor() {
        // Given
        String email = "test@example.com";
        String firstName = "John";
        String lastName = "Doe";

        // When
        Customer customer = new Customer(email, firstName, lastName);

        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getEmail()).isEqualTo(email);
        assertThat(customer.getFirstName()).isEqualTo(firstName);
        assertThat(customer.getLastName()).isEqualTo(lastName);
        assertThat(customer.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should set and get all basic fields")
    void shouldSetAndGetAllBasicFields() {
        // Given
        Customer customer = new Customer();
        User user = new User();
        String customerReference = "CUST_123";
        String email = "new@example.com";
        String firstName = "Jane";
        String lastName = "Smith";
        String phone = "+1234567890";
        String company = "Test Company";
        String authorizeNetId = "AUTH_123";

        // When
        customer.setUser(user);
        customer.setCustomerReference(customerReference);
        customer.setEmail(email);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPhone(phone);
        customer.setCompany(company);
        customer.setIsActive(false);
        customer.setAuthorizeNetCustomerProfileId(authorizeNetId);

        // Then
        assertThat(customer.getUser()).isEqualTo(user);
        assertThat(customer.getCustomerReference()).isEqualTo(customerReference);
        assertThat(customer.getCustomerId()).isEqualTo(customerReference);
        assertThat(customer.getEmail()).isEqualTo(email);
        assertThat(customer.getFirstName()).isEqualTo(firstName);
        assertThat(customer.getLastName()).isEqualTo(lastName);
        assertThat(customer.getPhone()).isEqualTo(phone);
        assertThat(customer.getCompany()).isEqualTo(company);
        assertThat(customer.getIsActive()).isFalse();
        assertThat(customer.getAuthorizeNetCustomerProfileId()).isEqualTo(authorizeNetId);
    }

    @Test
    @DisplayName("Should set and get billing address fields")
    void shouldSetAndGetBillingAddressFields() {
        // Given
        Customer customer = new Customer();
        String line1 = "123 Main St";
        String line2 = "Apt 4B";
        String city = "New York";
        String state = "NY";
        String postalCode = "10001";
        String country = "US";

        // When
        customer.setBillingAddressLine1(line1);
        customer.setBillingAddressLine2(line2);
        customer.setBillingCity(city);
        customer.setBillingState(state);
        customer.setBillingPostalCode(postalCode);
        customer.setBillingCountry(country);

        // Then
        assertThat(customer.getBillingAddressLine1()).isEqualTo(line1);
        assertThat(customer.getBillingAddressLine2()).isEqualTo(line2);
        assertThat(customer.getBillingCity()).isEqualTo(city);
        assertThat(customer.getBillingState()).isEqualTo(state);
        assertThat(customer.getBillingPostalCode()).isEqualTo(postalCode);
        assertThat(customer.getBillingCountry()).isEqualTo(country);
    }

    @Test
    @DisplayName("Should set and get shipping address fields")
    void shouldSetAndGetShippingAddressFields() {
        // Given
        Customer customer = new Customer();
        String line1 = "456 Oak Ave";
        String line2 = "Suite 200";
        String city = "Los Angeles";
        String state = "CA";
        String postalCode = "90210";
        String country = "US";

        // When
        customer.setShippingAddressLine1(line1);
        customer.setShippingAddressLine2(line2);
        customer.setShippingCity(city);
        customer.setShippingState(state);
        customer.setShippingPostalCode(postalCode);
        customer.setShippingCountry(country);

        // Then
        assertThat(customer.getShippingAddressLine1()).isEqualTo(line1);
        assertThat(customer.getShippingAddressLine2()).isEqualTo(line2);
        assertThat(customer.getShippingCity()).isEqualTo(city);
        assertThat(customer.getShippingState()).isEqualTo(state);
        assertThat(customer.getShippingPostalCode()).isEqualTo(postalCode);
        assertThat(customer.getShippingCountry()).isEqualTo(country);
    }

    @Test
    @DisplayName("Should validate successfully with valid data")
    void shouldValidateSuccessfullyWithValidData() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("valid@example.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");

        // When
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when email is null")
    void shouldFailValidationWhenEmailIsNull() {
        // Given
        Customer customer = new Customer();
        customer.setEmail(null);

        // When
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email is required");
    }

    @Test
    @DisplayName("Should fail validation when email is blank")
    void shouldFailValidationWhenEmailIsBlank() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("");

        // When
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "test@", "@example.com", "test.example.com"})
    @DisplayName("Should fail validation with invalid email formats")
    void shouldFailValidationWithInvalidEmailFormats(String invalidEmail) {
        // Given
        Customer customer = new Customer();
        customer.setEmail(invalidEmail);

        // When
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email must be valid");
    }

    @Test
    @DisplayName("Should fail validation when field exceeds max length")
    void shouldFailValidationWhenFieldExceedsMaxLength() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setCustomerReference("a".repeat(101));

        // When
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Customer reference must not exceed 100 characters");
    }

    @Test
    @DisplayName("Should return full name when both first and last names are present")
    void shouldReturnFullNameWhenBothNamesPresent() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");

        // When
        String fullName = customer.getFullName();

        // Then
        assertThat(fullName).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return first name when only first name is present")
    void shouldReturnFirstNameWhenOnlyFirstNamePresent() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setFirstName("John");
        customer.setLastName(null);

        // When
        String fullName = customer.getFullName();

        // Then
        assertThat(fullName).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return last name when only last name is present")
    void shouldReturnLastNameWhenOnlyLastNamePresent() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setFirstName(null);
        customer.setLastName("Doe");

        // When
        String fullName = customer.getFullName();

        // Then
        assertThat(fullName).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should return email when no names are present")
    void shouldReturnEmailWhenNoNamesPresent() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setFirstName(null);
        customer.setLastName(null);

        // When
        String fullName = customer.getFullName();

        // Then
        assertThat(fullName).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should add payment method correctly")
    void shouldAddPaymentMethodCorrectly() {
        // Given
        Customer customer = new Customer();
        PaymentMethod paymentMethod = new PaymentMethod();

        // When
        customer.addPaymentMethod(paymentMethod);

        // Then
        assertThat(customer.getPaymentMethods()).contains(paymentMethod);
        assertThat(paymentMethod.getCustomer()).isEqualTo(customer);
    }

    @Test
    @DisplayName("Should remove payment method correctly")
    void shouldRemovePaymentMethodCorrectly() {
        // Given
        Customer customer = new Customer();
        PaymentMethod paymentMethod = new PaymentMethod();
        customer.addPaymentMethod(paymentMethod);

        // When
        customer.removePaymentMethod(paymentMethod);

        // Then
        assertThat(customer.getPaymentMethods()).doesNotContain(paymentMethod);
        assertThat(paymentMethod.getCustomer()).isNull();
    }

    @Test
    @DisplayName("Should return default payment method when available")
    void shouldReturnDefaultPaymentMethodWhenAvailable() {
        // Given
        Customer customer = new Customer();
        PaymentMethod defaultMethod = new PaymentMethod();
        PaymentMethod nonDefaultMethod = new PaymentMethod();
        
        defaultMethod.setIsDefault(true);
        nonDefaultMethod.setIsDefault(false);
        
        customer.addPaymentMethod(nonDefaultMethod);
        customer.addPaymentMethod(defaultMethod);

        // When
        PaymentMethod result = customer.getDefaultPaymentMethod();

        // Then
        assertThat(result).isEqualTo(defaultMethod);
    }

    @Test
    @DisplayName("Should return null when no default payment method exists")
    void shouldReturnNullWhenNoDefaultPaymentMethodExists() {
        // Given
        Customer customer = new Customer();
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setIsDefault(false);
        customer.addPaymentMethod(paymentMethod);

        // When
        PaymentMethod result = customer.getDefaultPaymentMethod();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should detect complete billing address")
    void shouldDetectCompleteBillingAddress() {
        // Given
        Customer customer = new Customer();
        customer.setBillingAddressLine1("123 Main St");
        customer.setBillingCity("New York");
        customer.setBillingState("NY");
        customer.setBillingPostalCode("10001");

        // When
        boolean hasBillingAddress = customer.hasBillingAddress();

        // Then
        assertThat(hasBillingAddress).isTrue();
    }

    @Test
    @DisplayName("Should detect incomplete billing address")
    void shouldDetectIncompleteBillingAddress() {
        // Given
        Customer customer = new Customer();
        customer.setBillingAddressLine1("123 Main St");
        customer.setBillingCity("New York");
        // Missing state and postal code

        // When
        boolean hasBillingAddress = customer.hasBillingAddress();

        // Then
        assertThat(hasBillingAddress).isFalse();
    }

    @Test
    @DisplayName("Should detect complete shipping address")
    void shouldDetectCompleteShippingAddress() {
        // Given
        Customer customer = new Customer();
        customer.setShippingAddressLine1("456 Oak Ave");
        customer.setShippingCity("Los Angeles");
        customer.setShippingState("CA");
        customer.setShippingPostalCode("90210");

        // When
        boolean hasShippingAddress = customer.hasShippingAddress();

        // Then
        assertThat(hasShippingAddress).isTrue();
    }

    @Test
    @DisplayName("Should detect incomplete shipping address")
    void shouldDetectIncompleteShippingAddress() {
        // Given
        Customer customer = new Customer();
        customer.setShippingAddressLine1("456 Oak Ave");
        // Missing city, state, and postal code

        // When
        boolean hasShippingAddress = customer.hasShippingAddress();

        // Then
        assertThat(hasShippingAddress).isFalse();
    }

    @Test
    @DisplayName("Should set and get relationship collections")
    void shouldSetAndGetRelationshipCollections() {
        // Given
        Customer customer = new Customer();
        List<PaymentMethod> paymentMethods = new ArrayList<>();
        List<Order> orders = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();
        List<Subscription> subscriptions = new ArrayList<>();
        List<SubscriptionInvoice> invoices = new ArrayList<>();

        // When
        customer.setPaymentMethods(paymentMethods);
        customer.setOrders(orders);
        customer.setTransactions(transactions);
        customer.setSubscriptions(subscriptions);
        customer.setSubscriptionInvoices(invoices);

        // Then
        assertThat(customer.getPaymentMethods()).isEqualTo(paymentMethods);
        assertThat(customer.getOrders()).isEqualTo(orders);
        assertThat(customer.getTransactions()).isEqualTo(transactions);
        assertThat(customer.getSubscriptions()).isEqualTo(subscriptions);
        assertThat(customer.getSubscriptionInvoices()).isEqualTo(invoices);
    }

    @Test
    @DisplayName("Should handle null relationship collections")
    void shouldHandleNullRelationshipCollections() {
        // Given
        Customer customer = new Customer();

        // When
        customer.setPaymentMethods(null);
        customer.setOrders(null);
        customer.setTransactions(null);
        customer.setSubscriptions(null);
        customer.setSubscriptionInvoices(null);

        // Then
        assertThat(customer.getPaymentMethods()).isNull();
        assertThat(customer.getOrders()).isNull();
        assertThat(customer.getTransactions()).isNull();
        assertThat(customer.getSubscriptions()).isNull();
        assertThat(customer.getSubscriptionInvoices()).isNull();
    }

    @Test
    @DisplayName("Should validate with maximum length fields")
    void shouldValidateWithMaximumLengthFields() {
        // Given
        Customer customer = new Customer();
        customer.setCustomerReference("a".repeat(100));
        customer.setEmail("valid.test.email.for.maximum.length.validation@example.com"); // Simple valid email
        customer.setFirstName("a".repeat(100));
        customer.setLastName("a".repeat(100));
        customer.setPhone("a".repeat(20));
        customer.setCompany("a".repeat(255));
        customer.setBillingAddressLine1("a".repeat(255));
        customer.setBillingAddressLine2("a".repeat(255));
        customer.setBillingCity("a".repeat(100));
        customer.setBillingState("a".repeat(100));
        customer.setBillingPostalCode("a".repeat(20));
        customer.setBillingCountry("US");
        customer.setAuthorizeNetCustomerProfileId("a".repeat(50));

        // When
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle country code validation")
    void shouldHandleCountryCodeValidation() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setBillingCountry("USA"); // Too long

        // When
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Billing country must be 2 characters");
    }

    @Test
    @DisplayName("Should handle multiple payment methods")
    void shouldHandleMultiplePaymentMethods() {
        // Given
        Customer customer = new Customer();
        PaymentMethod method1 = new PaymentMethod();
        PaymentMethod method2 = new PaymentMethod();
        PaymentMethod method3 = new PaymentMethod();

        // When
        customer.addPaymentMethod(method1);
        customer.addPaymentMethod(method2);
        customer.addPaymentMethod(method3);

        // Then
        assertThat(customer.getPaymentMethods()).hasSize(3);
        assertThat(customer.getPaymentMethods()).contains(method1, method2, method3);
        assertThat(method1.getCustomer()).isEqualTo(customer);
        assertThat(method2.getCustomer()).isEqualTo(customer);
        assertThat(method3.getCustomer()).isEqualTo(customer);
    }

    @Test
    @DisplayName("Should handle empty string names in getFullName")
    void shouldHandleEmptyStringNamesInGetFullName() {
        // Given
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setFirstName("");
        customer.setLastName("");

        // When
        String fullName = customer.getFullName();

        // Then
        assertThat(fullName).isEqualTo("test@example.com");
    }
}
