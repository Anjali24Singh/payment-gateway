package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * API Documentation controller providing examples, guides, and best practices.
 * Helps developers understand how to integrate with the Payment Gateway API.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/docs")
@Tag(name = "API Documentation", description = "API guides, examples, and best practices")
public class ApiDocumentationController {

    /**
     * Get API integration guide with examples.
     */
    @GetMapping("/integration-guide")
    @Operation(
        summary = "Get API integration guide",
        description = "Returns comprehensive integration guide with code examples and best practices"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Integration guide retrieved successfully")
    })
    public ResponseEntity<ApiResponse<IntegrationGuide>> getIntegrationGuide() {
        IntegrationGuide guide = new IntegrationGuide(
            "Payment Gateway API Integration Guide",
            "1.0.0",
            getQuickStart(),
            getAuthenticationGuide(),
            getPaymentExamples(),
            getBestPractices(),
            getErrorHandling(),
            getWebhookGuide()
        );

        return ResponseEntity.ok(ApiResponse.success(guide));
    }

    /**
     * Get code examples for different programming languages.
     */
    @GetMapping("/examples/{language}")
    @Operation(
        summary = "Get code examples",
        description = "Returns code examples for specific programming language"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Code examples retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Language examples not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCodeExamples(
            @PathVariable String language) {
        
        Map<String, Object> examples = getCodeExamplesByLanguage(language);
        
        if (examples.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(examples));
    }

    /**
     * Get API testing scenarios and test cases.
     */
    @GetMapping("/testing-scenarios")
    @Operation(
        summary = "Get testing scenarios",
        description = "Returns comprehensive testing scenarios and test cases for API validation"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Testing scenarios retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<TestingScenario>>> getTestingScenarios() {
        List<TestingScenario> scenarios = Arrays.asList(
            new TestingScenario(
                "successful_payment",
                "Successful Payment Processing",
                "Test successful payment with valid card details",
                getSuccessfulPaymentSteps(),
                Map.of("card_number", "4111111111111111", "expected_status", "COMPLETED")
            ),
            new TestingScenario(
                "declined_payment",
                "Payment Decline Handling",
                "Test payment decline scenarios and error handling",
                getDeclinedPaymentSteps(),
                Map.of("card_number", "4000000000000002", "expected_status", "FAILED")
            ),
            new TestingScenario(
                "authentication",
                "Authentication Testing",
                "Test API authentication with valid and invalid credentials",
                getAuthenticationSteps(),
                Map.of("endpoint", "/api/v1/auth/login", "method", "POST")
            ),
            new TestingScenario(
                "refund_processing",
                "Refund Processing",
                "Test full and partial refund scenarios",
                getRefundSteps(),
                Map.of("original_transaction_required", true, "refund_types", Arrays.asList("FULL", "PARTIAL"))
            )
        );

        return ResponseEntity.ok(ApiResponse.success(scenarios));
    }

    /**
     * Get API changelog and version history.
     */
    @GetMapping("/changelog")
    @Operation(
        summary = "Get API changelog",
        description = "Returns API version history and changelog information"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Changelog retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ChangelogEntry>>> getChangelog() {
        List<ChangelogEntry> changelog = Arrays.asList(
            new ChangelogEntry(
                "v1.0.0",
                "2025-09-10",
                "Initial release",
                Arrays.asList(
                    "Core payment processing (purchase, authorize, capture, void, refund)",
                    "JWT-based authentication",
                    "Subscription management",
                    "Webhook support",
                    "OpenAPI documentation",
                    "Sandbox environment"
                ),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );

        return ResponseEntity.ok(ApiResponse.success(changelog));
    }

    // Helper methods for building guide content

    private QuickStart getQuickStart() {
        List<String> steps = Arrays.asList(
            "1. Register for a sandbox account at https://sandbox.talentica.com",
            "2. Obtain your API credentials (API key and secret)",
            "3. Authenticate using POST /api/v1/auth/login",
            "4. Include the Bearer token in all subsequent requests",
            "5. Test payment processing using POST /api/v1/payments/purchase",
            "6. Integrate webhooks for real-time notifications",
            "7. Test thoroughly in sandbox before going live"
        );

        String exampleAuth = """
            {
              "email": "admin@paymentgateway.com",
              "password": "Admin123!"
            }
            """;

        String examplePayment = """
            {
              "amount": 100.00,
              "currency": "USD",
              "payment_method": {
                "type": "CREDIT_CARD",
                "card_number": "4111111111111111",
                "expiry_month": "12",
                "expiry_year": "2028",
                "cvv": "123",
                "cardholder_name": "John Doe"
              },
              "customer": {
                "email": "customer@example.com",
                "first_name": "John",
                "last_name": "Doe"
              },
              "merchant_reference": "ORDER-12345"
            }
            """;

        return new QuickStart(steps, exampleAuth, examplePayment);
    }

    private AuthenticationGuide getAuthenticationGuide() {
        return new AuthenticationGuide(
            "JWT Bearer Token Authentication",
            Arrays.asList(
                "Obtain access token via POST /api/v1/auth/login",
                "Include token in Authorization header: Bearer <token>",
                "Token expires after 24 hours - use refresh token to renew",
                "Use HTTPS in production for secure token transmission"
            ),
            Map.of(
                "login_endpoint", "/api/v1/auth/login",
                "refresh_endpoint", "/api/v1/auth/refresh",
                "token_header", "Authorization: Bearer <access_token>"
            )
        );
    }

    private List<PaymentExample> getPaymentExamples() {
        return Arrays.asList(
            new PaymentExample(
                "purchase",
                "Direct Purchase",
                "Process a direct purchase (auth + capture)",
                "POST /api/v1/payments/purchase",
                """
                {
                  "amount": 100.00,
                  "currency": "USD",
                  "payment_method": {
                    "type": "CREDIT_CARD",
                    "card_number": "4111111111111111",
                    "expiry_month": "12",
                    "expiry_year": "2028",
                    "cvv": "123"
                  }
                }
                """
            ),
            new PaymentExample(
                "authorize",
                "Authorization Only",
                "Authorize payment without capturing funds",
                "POST /api/v1/payments/authorize",
                """
                {
                  "amount": 150.00,
                  "currency": "USD",
                  "payment_method": {
                    "type": "CREDIT_CARD",
                    "card_number": "4111111111111111",
                    "expiry_month": "12",
                    "expiry_year": "2028",
                    "cvv": "123"
                  }
                }
                """
            )
        );
    }

    private List<String> getBestPractices() {
        return Arrays.asList(
            "Always use HTTPS for API communications",
            "Store API credentials securely and rotate them regularly",
            "Implement idempotency using unique request IDs",
            "Use correlation IDs for request tracking and debugging",
            "Implement proper error handling and retry logic",
            "Validate input data on both client and server side",
            "Use webhooks for real-time payment status updates",
            "Implement rate limiting and request throttling",
            "Log all API interactions for audit and debugging",
            "Test thoroughly in sandbox before production deployment"
        );
    }

    private ErrorHandlingGuide getErrorHandling() {
        return new ErrorHandlingGuide(
            "Standard HTTP status codes with detailed error responses",
            Map.of(
                "400", "Bad Request - Invalid request format or parameters",
                "401", "Unauthorized - Invalid or missing authentication",
                "403", "Forbidden - Insufficient permissions",
                "404", "Not Found - Resource not found",
                "422", "Unprocessable Entity - Payment processing failed",
                "429", "Too Many Requests - Rate limit exceeded",
                "500", "Internal Server Error - Unexpected system error"
            ),
            """
            {
              "status": 400,
              "error_code": "VALIDATION_ERROR",
              "message": "Request validation failed",
              "correlation_id": "corr-12345678",
              "timestamp": "2025-09-10T10:30:00",
              "validation_errors": [
                {
                  "field": "amount",
                  "code": "INVALID_VALUE",
                  "message": "Amount must be greater than 0"
                }
              ]
            }
            """
        );
    }

    private WebhookGuide getWebhookGuide() {
        return new WebhookGuide(
            "Configure webhook endpoints to receive real-time payment notifications",
            Arrays.asList(
                "payment.completed",
                "payment.failed",
                "payment.refunded",
                "subscription.created",
                "subscription.cancelled"
            ),
            """
            {
              "event_type": "payment.completed",
              "event_id": "evt_123456789",
              "timestamp": "2025-09-10T10:30:00Z",
              "data": {
                "transaction_id": "txn_987654321",
                "amount": 100.00,
                "currency": "USD",
                "status": "COMPLETED"
              }
            }
            """
        );
    }

    private Map<String, Object> getCodeExamplesByLanguage(String language) {
        Map<String, Object> examples = new HashMap<>();
        
        switch (language.toLowerCase()) {
            case "javascript":
                examples.put("authentication", getJavaScriptAuthExample());
                examples.put("payment", getJavaScriptPaymentExample());
                break;
            case "python":
                examples.put("authentication", getPythonAuthExample());
                examples.put("payment", getPythonPaymentExample());
                break;
            case "java":
                examples.put("authentication", getJavaAuthExample());
                examples.put("payment", getJavaPaymentExample());
                break;
            case "curl":
                examples.put("authentication", getCurlAuthExample());
                examples.put("payment", getCurlPaymentExample());
                break;
        }
        
        return examples;
    }

    // Test scenario step definitions
    private List<String> getSuccessfulPaymentSteps() {
        return Arrays.asList(
            "1. Authenticate using valid credentials",
            "2. Send POST request to /api/v1/payments/purchase",
            "3. Use test card number 4111111111111111",
            "4. Verify response status is 200",
            "5. Verify transaction status is COMPLETED",
            "6. Check that transaction ID is returned"
        );
    }

    private List<String> getDeclinedPaymentSteps() {
        return Arrays.asList(
            "1. Authenticate using valid credentials",
            "2. Send POST request to /api/v1/payments/purchase",
            "3. Use test card number 4000000000000002",
            "4. Verify response status is 422",
            "5. Verify error code indicates payment decline",
            "6. Check error message for decline reason"
        );
    }

    private List<String> getAuthenticationSteps() {
        return Arrays.asList(
            "1. Send POST request to /api/v1/auth/login with valid credentials",
            "2. Verify response contains access_token and refresh_token",
            "3. Test API call with valid token in Authorization header",
            "4. Test API call with invalid token - should return 401",
            "5. Test token refresh using refresh_token",
            "6. Verify refreshed token works for API calls"
        );
    }

    private List<String> getRefundSteps() {
        return Arrays.asList(
            "1. Create a successful payment transaction",
            "2. Send POST request to /api/v1/payments/refund",
            "3. Reference original transaction ID",
            "4. Test full refund (no amount specified)",
            "5. Test partial refund (specify amount)",
            "6. Verify refund status and amount"
        );
    }

    // Code examples for different languages
    private String getJavaScriptAuthExample() {
        return """
            const response = await fetch('https://api.talentica.com/v1/auth/login', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                email: 'admin@paymentgateway.com',
                password: 'Admin123!'
              })
            });
            
            const data = await response.json();
            const token = data.access_token;
            """;
    }

    private String getJavaScriptPaymentExample() {
        return """
            const response = await fetch('https://api.talentica.com/v1/payments/purchase', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
              },
              body: JSON.stringify({
                amount: 100.00,
                currency: 'USD',
                payment_method: {
                  type: 'CREDIT_CARD',
                  card_number: '4111111111111111',
                  expiry_month: '12',
                  expiry_year: '2028',
                  cvv: '123'
                }
              })
            });
            """;
    }

    private String getPythonAuthExample() {
        return """
            import requests
            
            response = requests.post('https://api.talentica.com/v1/auth/login', 
                json={
                    'email': 'admin@paymentgateway.com',
                    'password': 'Admin123!'
                })
            
            token = response.json()['access_token']
            """;
    }

    private String getPythonPaymentExample() {
        return """
            headers = {'Authorization': f'Bearer {token}'}
            
            response = requests.post('https://api.talentica.com/v1/payments/purchase',
                headers=headers,
                json={
                    'amount': 100.00,
                    'currency': 'USD',
                    'payment_method': {
                        'type': 'CREDIT_CARD',
                        'card_number': '4111111111111111',
                        'expiry_month': '12',
                        'expiry_year': '2028',
                        'cvv': '123'
                    }
                })
            """;
    }

    private String getJavaAuthExample() {
        return """
            // Using Spring RestTemplate
            RestTemplate restTemplate = new RestTemplate();
            
            AuthRequest authRequest = new AuthRequest("admin@paymentgateway.com", "Admin123!");
            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "https://api.talentica.com/v1/auth/login", 
                authRequest, 
                AuthResponse.class
            );
            
            String token = response.getBody().getAccessToken();
            """;
    }

    private String getJavaPaymentExample() {
        return """
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setAmount(new BigDecimal("100.00"));
            paymentRequest.setCurrency("USD");
            
            HttpEntity<PaymentRequest> entity = new HttpEntity<>(paymentRequest, headers);
            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                "https://api.talentica.com/v1/payments/purchase",
                entity,
                PaymentResponse.class
            );
            """;
    }

    private String getCurlAuthExample() {
        return """
            curl -X POST https://api.talentica.com/v1/auth/login \\
              -H "Content-Type: application/json" \\
              -d '{
                "email": "admin@paymentgateway.com",
                "password": "Admin123!"
              }'
            """;
    }

    private String getCurlPaymentExample() {
        return """
            curl -X POST https://api.talentica.com/v1/payments/purchase \\
              -H "Content-Type: application/json" \\
              -H "Authorization: Bearer YOUR_TOKEN_HERE" \\
              -d '{
                "amount": 100.00,
                "currency": "USD",
                "payment_method": {
                  "type": "CREDIT_CARD",
                  "card_number": "4111111111111111",
                  "expiry_month": "12",
                  "expiry_year": "2028",
                  "cvv": "123"
                }
              }'
            """;
    }

    // Data classes for response structures
    public static class IntegrationGuide {
        private String title;
        private String version;
        private QuickStart quickStart;
        private AuthenticationGuide authentication;
        private List<PaymentExample> paymentExamples;
        private List<String> bestPractices;
        private ErrorHandlingGuide errorHandling;
        private WebhookGuide webhooks;

        public IntegrationGuide(String title, String version, QuickStart quickStart, 
                               AuthenticationGuide authentication, List<PaymentExample> paymentExamples,
                               List<String> bestPractices, ErrorHandlingGuide errorHandling, 
                               WebhookGuide webhooks) {
            this.title = title;
            this.version = version;
            this.quickStart = quickStart;
            this.authentication = authentication;
            this.paymentExamples = paymentExamples;
            this.bestPractices = bestPractices;
            this.errorHandling = errorHandling;
            this.webhooks = webhooks;
        }

        // Getters
        public String getTitle() { return title; }
        public String getVersion() { return version; }
        public QuickStart getQuickStart() { return quickStart; }
        public AuthenticationGuide getAuthentication() { return authentication; }
        public List<PaymentExample> getPaymentExamples() { return paymentExamples; }
        public List<String> getBestPractices() { return bestPractices; }
        public ErrorHandlingGuide getErrorHandling() { return errorHandling; }
        public WebhookGuide getWebhooks() { return webhooks; }
    }

    public static class QuickStart {
        private List<String> steps;
        private String authenticationExample;
        private String paymentExample;

        public QuickStart(List<String> steps, String authenticationExample, String paymentExample) {
            this.steps = steps;
            this.authenticationExample = authenticationExample;
            this.paymentExample = paymentExample;
        }

        // Getters
        public List<String> getSteps() { return steps; }
        public String getAuthenticationExample() { return authenticationExample; }
        public String getPaymentExample() { return paymentExample; }
    }

    public static class AuthenticationGuide {
        private String type;
        private List<String> instructions;
        private Map<String, String> endpoints;

        public AuthenticationGuide(String type, List<String> instructions, Map<String, String> endpoints) {
            this.type = type;
            this.instructions = instructions;
            this.endpoints = endpoints;
        }

        // Getters
        public String getType() { return type; }
        public List<String> getInstructions() { return instructions; }
        public Map<String, String> getEndpoints() { return endpoints; }
    }

    public static class PaymentExample {
        private String type;
        private String title;
        private String description;
        private String endpoint;
        private String example;

        public PaymentExample(String type, String title, String description, String endpoint, String example) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.endpoint = endpoint;
            this.example = example;
        }

        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getEndpoint() { return endpoint; }
        public String getExample() { return example; }
    }

    public static class ErrorHandlingGuide {
        private String description;
        private Map<String, String> statusCodes;
        private String errorResponseExample;

        public ErrorHandlingGuide(String description, Map<String, String> statusCodes, String errorResponseExample) {
            this.description = description;
            this.statusCodes = statusCodes;
            this.errorResponseExample = errorResponseExample;
        }

        // Getters
        public String getDescription() { return description; }
        public Map<String, String> getStatusCodes() { return statusCodes; }
        public String getErrorResponseExample() { return errorResponseExample; }
    }

    public static class WebhookGuide {
        private String description;
        private List<String> eventTypes;
        private String examplePayload;

        public WebhookGuide(String description, List<String> eventTypes, String examplePayload) {
            this.description = description;
            this.eventTypes = eventTypes;
            this.examplePayload = examplePayload;
        }

        // Getters
        public String getDescription() { return description; }
        public List<String> getEventTypes() { return eventTypes; }
        public String getExamplePayload() { return examplePayload; }
    }

    public static class TestingScenario {
        private String id;
        private String title;
        private String description;
        private List<String> steps;
        private Map<String, Object> testData;

        public TestingScenario(String id, String title, String description, List<String> steps, Map<String, Object> testData) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.steps = steps;
            this.testData = testData;
        }

        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public List<String> getSteps() { return steps; }
        public Map<String, Object> getTestData() { return testData; }
    }

    public static class ChangelogEntry {
        private String version;
        private String date;
        private String description;
        private List<String> added;
        private List<String> changed;
        private List<String> removed;

        public ChangelogEntry(String version, String date, String description, List<String> added, List<String> changed, List<String> removed) {
            this.version = version;
            this.date = date;
            this.description = description;
            this.added = added;
            this.changed = changed;
            this.removed = removed;
        }

        // Getters
        public String getVersion() { return version; }
        public String getDate() { return date; }
        public String getDescription() { return description; }
        public List<String> getAdded() { return added; }
        public List<String> getChanged() { return changed; }
        public List<String> getRemoved() { return removed; }
    }
}
