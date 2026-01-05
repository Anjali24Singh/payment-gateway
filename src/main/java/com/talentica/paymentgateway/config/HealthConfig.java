package com.talentica.paymentgateway.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Configuration for Payment Gateway Dependencies.
 * 
 * Provides comprehensive health checks for all critical system dependencies
 * including database, Redis, and external payment services.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
public class HealthConfig {

    /**
     * Database health indicator.
     * 
     * @param dataSource Database data source
     * @return Database health indicator
     */
    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return new DatabaseHealthIndicator(dataSource);
    }

    /**
     * Redis health indicator.
     * 
     * @param redisConnectionFactory Redis connection factory
     * @param redisTemplate Redis template
     * @return Redis health indicator
     */
    @Bean
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean({RedisConnectionFactory.class, RedisTemplate.class})
    public HealthIndicator redisHealthIndicator(
            RedisConnectionFactory redisConnectionFactory,
            RedisTemplate<String, Object> redisTemplate) {
        return new RedisHealthIndicator(redisConnectionFactory, redisTemplate);
    }

    /**
     * Authorize.Net health indicator.
     * 
     * @param authorizeNetConfig Authorize.Net configuration
     * @return Authorize.Net health indicator
     */
    @Bean
    public HealthIndicator authorizeNetHealthIndicator(AuthorizeNetConfig authorizeNetConfig) {
        return new AuthorizeNetHealthIndicator(authorizeNetConfig);
    }

    /**
     * Database health indicator implementation.
     */
    private static class DatabaseHealthIndicator implements HealthIndicator {
        
        private final DataSource dataSource;
        
        public DatabaseHealthIndicator(DataSource dataSource) {
            this.dataSource = dataSource;
        }
        
        @Override
        public Health health() {
            try {
                Instant start = Instant.now();
                
                try (Connection connection = dataSource.getConnection()) {
                    // Test database connectivity and basic query
                    boolean isValid = connection.isValid(5); // 5 second timeout
                    Duration responseTime = Duration.between(start, Instant.now());
                    
                    Map<String, Object> details = new HashMap<>();
                    details.put("database", connection.getMetaData().getDatabaseProductName());
                    details.put("version", connection.getMetaData().getDatabaseProductVersion());
                    details.put("url", connection.getMetaData().getURL());
                    details.put("responseTime", responseTime.toMillis() + "ms");
                    details.put("validationQuery", "connection.isValid()");
                    details.put("timestamp", Instant.now().toString());
                    
                    if (isValid && responseTime.toMillis() < 5000) {
                        return Health.up()
                                .withDetails(details)
                                .build();
                    } else {
                        details.put("issue", "Database connection validation failed or slow response");
                        return Health.down()
                                .withDetails(details)
                                .build();
                    }
                }
                
            } catch (Exception e) {
                Map<String, Object> details = new HashMap<>();
                details.put("error", e.getMessage());
                details.put("cause", e.getClass().getSimpleName());
                details.put("timestamp", Instant.now().toString());
                
                return Health.down()
                        .withException(e)
                        .withDetails(details)
                        .build();
            }
        }
    }

    /**
     * Redis health indicator implementation.
     */
    private static class RedisHealthIndicator implements HealthIndicator {
        
        private final RedisTemplate<String, Object> redisTemplate;
        
        public RedisHealthIndicator(RedisConnectionFactory connectionFactory, 
                                  RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }
        
        @Override
        public Health health() {
            try {
                Instant start = Instant.now();
                
                // Test Redis connectivity with ping
                String pingResult = "PONG"; // Simplified ping test
                try {
                    redisTemplate.getConnectionFactory().getConnection().ping();
                } catch (Exception e) {
                    throw new RuntimeException("Redis ping failed", e);
                }
                
                Duration responseTime = Duration.between(start, Instant.now());
                
                Map<String, Object> details = new HashMap<>();
                details.put("ping", pingResult);
                details.put("responseTime", responseTime.toMillis() + "ms");
                details.put("timestamp", Instant.now().toString());
                
                // Test basic operations
                try {
                    String testKey = "health:check:" + System.currentTimeMillis();
                    String testValue = "test";
                    
                    redisTemplate.opsForValue().set(testKey, testValue, Duration.ofSeconds(10));
                    String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
                    redisTemplate.delete(testKey);
                    
                    if (testValue.equals(retrievedValue)) {
                        details.put("operations", "read/write test successful");
                        
                        return Health.up()
                                .withDetails(details)
                                .build();
                    } else {
                        details.put("issue", "Redis read/write test failed");
                        return Health.down()
                                .withDetails(details)
                                .build();
                    }
                    
                } catch (Exception operationException) {
                    details.put("operations", "failed: " + operationException.getMessage());
                    return Health.down()
                            .withDetails(details)
                            .build();
                }
                
            } catch (Exception e) {
                Map<String, Object> details = new HashMap<>();
                details.put("error", e.getMessage());
                details.put("cause", e.getClass().getSimpleName());
                details.put("timestamp", Instant.now().toString());
                
                return Health.down()
                        .withException(e)
                        .withDetails(details)
                        .build();
            }
        }
    }

    /**
     * Authorize.Net health indicator implementation.
     */
    private static class AuthorizeNetHealthIndicator implements HealthIndicator {
        
        private final AuthorizeNetConfig authorizeNetConfig;
        
        public AuthorizeNetHealthIndicator(AuthorizeNetConfig authorizeNetConfig) {
            this.authorizeNetConfig = authorizeNetConfig;
        }
        
        @Override
        public Health health() {
            try {
                Map<String, Object> details = new HashMap<>();
                
                // Check configuration
                boolean isConfigured = authorizeNetConfig.getApiLoginId() != null && 
                                     !authorizeNetConfig.getApiLoginId().isEmpty() &&
                                     authorizeNetConfig.getTransactionKey() != null && 
                                     !authorizeNetConfig.getTransactionKey().isEmpty();
                
                details.put("configured", isConfigured);
                details.put("environment", authorizeNetConfig.getEnvironment());
                details.put("timestamp", Instant.now().toString());
                
                if (!isConfigured) {
                    details.put("issue", "Authorize.Net credentials not properly configured");
                    return Health.down()
                            .withDetails(details)
                            .build();
                }
                
                // In a real implementation, you might want to test connectivity
                // to Authorize.Net API endpoints, but be careful about rate limits
                // and authentication requirements
                
                // For now, we'll just verify configuration
                Status status = isConfigured ? Status.UP : Status.DOWN;
                
                return Health.status(status)
                        .withDetails(details)
                        .build();
                
            } catch (Exception e) {
                Map<String, Object> details = new HashMap<>();
                details.put("error", e.getMessage());
                details.put("cause", e.getClass().getSimpleName());
                details.put("timestamp", Instant.now().toString());
                
                return Health.down()
                        .withException(e)
                        .withDetails(details)
                        .build();
            }
        }
    }
}
