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
 * Minimal test configuration for integration tests.
 * Only mocks infrastructure components, not business services.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@TestConfiguration
public class IntegrationTestConfig {

    /**
     * Provide a mock RedisConnectionFactory for integration tests.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    /**
     * Provide a mock RedisTemplate for integration tests.
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    /**
     * Provide PasswordEncoder for integration tests.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Mock essential services that business logic depends on but aren't part of integration test scope
    @MockBean
    private MetricsService metricsService;
    
    @MockBean
    private RequestTrackingService requestTrackingService;
    
    @MockBean
    private NotificationService notificationService;
}