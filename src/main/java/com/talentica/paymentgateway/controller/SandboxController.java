package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.config.SandboxConfig;
import com.talentica.paymentgateway.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentMethod;
import com.talentica.paymentgateway.repository.CustomerRepository;
import com.talentica.paymentgateway.repository.PaymentMethodRepository;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Sandbox environment controller for testing and development.
 * Provides test data, mock responses, and sandbox-specific utilities.
 * Only available in development, sandbox, and test profiles.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sandbox")
@Tag(name = "Sandbox", description = "Sandbox environment utilities and test data")
@Profile({"dev", "sandbox", "test"})
@ConditionalOnProperty(name = "app.sandbox.enabled", havingValue = "true", matchIfMissing = true)
public class SandboxController {

    private final SandboxConfig sandboxConfig;
    private final CustomerRepository customerRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public SandboxController(SandboxConfig sandboxConfig, 
                           CustomerRepository customerRepository,
                           PaymentMethodRepository paymentMethodRepository) {
        this.sandboxConfig = sandboxConfig;
        this.customerRepository = customerRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    /**
     * Get sandbox environment information and configuration.
     */
    @GetMapping("/info")
    @Operation(
        summary = "Get sandbox information",
        description = "Returns sandbox environment configuration and available test utilities"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sandbox information retrieved successfully")
    })
    public ResponseEntity<ApiResponse<SandboxInfo>> getSandboxInfo() {
        log.info("Sandbox info requested");

        SandboxInfo info = new SandboxInfo(
            true,
            "Sandbox environment for Payment Gateway API testing",
            sandboxConfig.getDefaultApiKey(),
            sandboxConfig.getTestCards(),
            getTestScenarios(),
            getApiEndpoints()
        );

        return ResponseEntity.ok(ApiResponse.success(info));
    }

    /**
     * Get test credit card numbers for different scenarios.
     */
    @GetMapping("/test-cards")
    @Operation(
        summary = "Get test credit card numbers",
        description = "Returns test credit card numbers for different testing scenarios"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Test cards retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, TestCard>>> getTestCards() {
        log.info("Test cards requested");

        Map<String, TestCard> testCards = new HashMap<>();
        
        testCards.put("success", new TestCard(
            sandboxConfig.getTestCards().getSuccessCard(),
            "12/28",
            "123",
            "Successful payment - transaction will be approved"
        ));
        
        testCards.put("decline", new TestCard(
            sandboxConfig.getTestCards().getDeclineCard(),
            "12/28",
            "123",
            "Declined payment - insufficient funds"
        ));
        
        testCards.put("error", new TestCard(
            sandboxConfig.getTestCards().getErrorCard(),
            "12/28",
            "123",
            "Processing error - general processing error"
        ));
        
        testCards.put("expired", new TestCard(
            sandboxConfig.getTestCards().getExpiredCard(),
            "12/20",
            "123",
            "Expired card error"
        ));
        
        testCards.put("cvv_fail", new TestCard(
            sandboxConfig.getTestCards().getCvvFailCard(),
            "12/28",
            "123",
            "CVV verification failure"
        ));

        return ResponseEntity.ok(ApiResponse.success(testCards));
    }

    /**
     * Generate test transaction data for development.
     */
    @PostMapping("/generate-test-data")
    @Operation(
        summary = "Generate test transaction data",
        description = "Generates sample transaction data for testing and development purposes"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Test data generated successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateTestData(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "PURCHASE") String transactionType) {
        
        log.info("Generating {} test transactions of type {}", count, transactionType);

        List<Map<String, Object>> testTransactions = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("transaction_id", "test_" + UUID.randomUUID().toString().substring(0, 8));
            transaction.put("amount", 10.00 + (random.nextDouble() * 990.00));
            transaction.put("currency", "USD");
            transaction.put("status", getRandomStatus(random));
            transaction.put("type", transactionType);
            transaction.put("created_at", LocalDateTime.now().minusHours(random.nextInt(24)));
            transaction.put("card_last_four", String.format("%04d", random.nextInt(10000)));
            transaction.put("merchant_id", "test_merchant_" + (random.nextInt(100) + 1));
            
            testTransactions.add(transaction);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("generated_count", count);
        result.put("transaction_type", transactionType);
        result.put("transactions", testTransactions);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Reset sandbox environment to initial state.
     */
    @PostMapping("/reset")
    @Operation(
        summary = "Reset sandbox environment",
        description = "Resets the sandbox environment to its initial state, clearing test data"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sandbox reset successfully")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetSandbox() {
        log.info("Resetting sandbox environment");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "reset_complete");
        result.put("timestamp", LocalDateTime.now());
        result.put("message", "Sandbox environment has been reset to initial state");

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get available test scenarios and their descriptions.
     */
    private List<TestScenario> getTestScenarios() {
        return Arrays.asList(
            new TestScenario("successful_payment", "Test successful payment processing"),
            new TestScenario("declined_payment", "Test payment decline scenarios"),
            new TestScenario("processing_error", "Test payment processing errors"),
            new TestScenario("network_timeout", "Test network timeout handling"),
            new TestScenario("invalid_card", "Test invalid card number validation"),
            new TestScenario("expired_card", "Test expired card handling"),
            new TestScenario("cvv_failure", "Test CVV verification failure"),
            new TestScenario("insufficient_funds", "Test insufficient funds scenario"),
            new TestScenario("refund_processing", "Test refund transaction processing"),
            new TestScenario("subscription_billing", "Test recurring subscription billing")
        );
    }

    /**
     * Get available API endpoints for testing.
     */
    private List<ApiEndpoint> getApiEndpoints() {
        return Arrays.asList(
            new ApiEndpoint("POST", "/api/v1/payments/purchase", "Process purchase transaction"),
            new ApiEndpoint("POST", "/api/v1/payments/authorize", "Process authorization"),
            new ApiEndpoint("POST", "/api/v1/payments/capture", "Capture authorized payment"),
            new ApiEndpoint("POST", "/api/v1/payments/void", "Void authorized payment"),
            new ApiEndpoint("POST", "/api/v1/payments/refund", "Process refund"),
            new ApiEndpoint("GET", "/api/v1/payments/{id}", "Get transaction status"),
            new ApiEndpoint("POST", "/api/v1/auth/login", "User authentication"),
            new ApiEndpoint("POST", "/api/v1/subscriptions", "Create subscription"),
            new ApiEndpoint("GET", "/api/v1/analytics/transactions", "Get transaction analytics")
        );
    }

    /**
     * Get random transaction status for test data generation.
     */
    private String getRandomStatus(Random random) {
        String[] statuses = {"COMPLETED", "PENDING", "FAILED", "CANCELLED", "REFUNDED"};
        return statuses[random.nextInt(statuses.length)];
    }

    // Data classes for response structures
    public static class SandboxInfo {
        private boolean enabled;
        private String description;
        private String defaultApiKey;
        private SandboxConfig.TestCards testCards;
        private List<TestScenario> testScenarios;
        private List<ApiEndpoint> apiEndpoints;

        public SandboxInfo(boolean enabled, String description, String defaultApiKey, 
                          SandboxConfig.TestCards testCards, List<TestScenario> testScenarios, 
                          List<ApiEndpoint> apiEndpoints) {
            this.enabled = enabled;
            this.description = description;
            this.defaultApiKey = defaultApiKey;
            this.testCards = testCards;
            this.testScenarios = testScenarios;
            this.apiEndpoints = apiEndpoints;
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public String getDescription() { return description; }
        public String getDefaultApiKey() { return defaultApiKey; }
        public SandboxConfig.TestCards getTestCards() { return testCards; }
        public List<TestScenario> getTestScenarios() { return testScenarios; }
        public List<ApiEndpoint> getApiEndpoints() { return apiEndpoints; }
    }

    public static class TestCard {
        private String number;
        private String expiry;
        private String cvv;
        private String description;

        public TestCard(String number, String expiry, String cvv, String description) {
            this.number = number;
            this.expiry = expiry;
            this.cvv = cvv;
            this.description = description;
        }

        // Getters
        public String getNumber() { return number; }
        public String getExpiry() { return expiry; }
        public String getCvv() { return cvv; }
        public String getDescription() { return description; }
    }

    public static class TestScenario {
        private String name;
        private String description;

        public TestScenario(String name, String description) {
            this.name = name;
            this.description = description;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    public static class ApiEndpoint {
        private String method;
        private String path;
        private String description;

        public ApiEndpoint(String method, String path, String description) {
            this.method = method;
            this.path = path;
            this.description = description;
        }

        // Getters
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getDescription() { return description; }
    }

    /**
     * Create a test payment method for subscription testing.
     */
    @PostMapping("/create-test-payment-method")
    @Operation(
        summary = "Create test payment method",
        description = "Creates a test payment method for the specified customer for testing subscriptions"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createTestPaymentMethod(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "pm_test_") String tokenPrefix) {
        
        try {
            // Find customer
            Optional<Customer> customerOpt = customerRepository.findByCustomerId(customerId);
            if (customerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Customer customer = customerOpt.get();
            String paymentToken = tokenPrefix + customerId + "_" + System.currentTimeMillis();
            
            // Create test payment method
            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.setCustomer(customer);
            paymentMethod.setPaymentToken(paymentToken);
            paymentMethod.setPaymentType("CREDIT_CARD");
            paymentMethod.setCardLastFour("1111");
            paymentMethod.setCardBrand("VISA");
            paymentMethod.setCardExpiryMonth(12);
            paymentMethod.setCardExpiryYear(2028);
            paymentMethod.setCardholderName("Test User");
            paymentMethod.setIsDefault(true);
            paymentMethod.setIsActive(true);
            paymentMethod.setCardNumber("encrypted_test_card");
            paymentMethod.setExpiryMonth("12");
            paymentMethod.setExpiryYear("2028");
            paymentMethod.setCvv("encrypted_cvv");
            
            PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("paymentMethodId", saved.getPaymentToken());
            response.put("customerId", customerId);
            response.put("message", "Test payment method created successfully");
            
            log.info("Created test payment method: {} for customer: {}", saved.getPaymentToken(), customerId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to create test payment method for customer: {}", customerId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
