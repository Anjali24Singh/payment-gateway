package com.talentica.paymentgateway.config;

import com.talentica.paymentgateway.service.*;
import com.talentica.paymentgateway.repository.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for unit and integration tests.
 * Provides mock beans and test-specific configurations.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provide a mock RedisConnectionFactory for tests that don't need real Redis.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    /**
     * Provide a mock RedisTemplate for tests that don't need real Redis.
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    /**
     * Provide PasswordEncoder for tests.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Mock Services
    @MockBean
    private UserService userService;
    
    @MockBean 
    private PaymentService paymentService;
    
    @MockBean
    private JwtService jwtService;
    
    @MockBean
    private CustomerService customerService;
    
    @MockBean
    private MetricsService metricsService;
    
    @MockBean
    private RateLimitService rateLimitService;
    
    @MockBean
    private NotificationService notificationService;
    
    @MockBean
    private ApiKeyService apiKeyService;
    
    @MockBean
    private AnalyticsService analyticsService;
    
    @MockBean
    private AuthorizeNetARBService authorizeNetARBService;
    
    @MockBean
    private AuthorizeNetCustomerService authorizeNetCustomerService;
    
    @MockBean
    private PaymentErrorHandler paymentErrorHandler;
    
    @MockBean
    private ProrationService prorationService;
    
    @MockBean
    private ReportExportService reportExportService;
    
    @MockBean
    private RequestTrackingService requestTrackingService;
    
    @MockBean
    private SubscriptionBillingEngine subscriptionBillingEngine;
    
    @MockBean
    private SubscriptionPlanService subscriptionPlanService;
    
    @MockBean
    private SubscriptionService subscriptionService;
    
    @MockBean
    private WebhookProcessingService webhookProcessingService;
    
    @MockBean
    private WebhookRetryService webhookRetryService;
    
    // Mock Repositories
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private CustomerRepository customerRepository;
    
    @MockBean
    private TransactionRepository transactionRepository;
    
    @MockBean
    private ApiKeyRepository apiKeyRepository;
    
    @MockBean
    private AuditLogRepository auditLogRepository;
    
    @MockBean
    private OrderRepository orderRepository;
    
    @MockBean
    private PaymentMethodRepository paymentMethodRepository;
    
    @MockBean
    private SubscriptionRepository subscriptionRepository;
    
    @MockBean
    private SubscriptionPlanRepository subscriptionPlanRepository;
    
    @MockBean
    private SubscriptionInvoiceRepository subscriptionInvoiceRepository;
    
    @MockBean
    private WebhookRepository webhookRepository;
}
