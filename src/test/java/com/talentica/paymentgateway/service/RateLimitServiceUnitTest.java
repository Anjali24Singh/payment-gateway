package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.config.ApplicationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService.
 * Simplified tests focusing on core functionality and actual service behavior.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceUnitTest {

    @Mock(lenient = true)
    private RedisTemplate<String, Object> redisTemplate;

    @Mock(lenient = true)
    private ValueOperations<String, Object> valueOperations;

    @Mock(lenient = true)
    private ApplicationConfig.AppProperties appProperties;

    @Mock(lenient = true)
    private ApplicationConfig.AppProperties.RateLimit rateLimitConfig;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(appProperties.getRateLimit()).thenReturn(rateLimitConfig);
        when(rateLimitConfig.isEnabled()).thenReturn(true);
        when(rateLimitConfig.getDefaultLimit()).thenReturn(100);
        when(rateLimitConfig.getBurstLimit()).thenReturn(150);
        
        rateLimitService = new RateLimitService(redisTemplate, appProperties);
    }

    @Test
    void constructor_WithoutRedis_ShouldInitializeSuccessfully() {
        // When
        RateLimitService serviceWithoutRedis = new RateLimitService(null, appProperties);

        // Then
        assertThat(serviceWithoutRedis).isNotNull();
    }

    @Test
    void isAllowed_WithRateLimitingDisabled_ShouldAlwaysAllow() {
        // Given
        when(rateLimitConfig.isEnabled()).thenReturn(false);
        String identifier = "test_user";

        // When
        RateLimitService.RateLimitResult result = rateLimitService.isAllowed(identifier, 10, 15);

        // Then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getLimit()).isEqualTo(10);
        assertThat(result.getRemaining()).isEqualTo(10);
    }

    @Test
    void isAllowed_WithEmptyIdentifier_ShouldDenyRequest() {
        // When
        RateLimitService.RateLimitResult result = rateLimitService.isAllowed("", 10, 15);

        // Then
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isEqualTo(0);
    }

    @Test
    void isAllowed_WithNullIdentifier_ShouldDenyRequest() {
        // When
        RateLimitService.RateLimitResult result = rateLimitService.isAllowed(null, 10, 15);

        // Then
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isEqualTo(0);
    }

    @Test
    void isAllowed_WithoutRedis_ShouldAllowAllRequests() {
        // Given
        RateLimitService serviceWithoutRedis = new RateLimitService(null, appProperties);
        String identifier = "test_user";

        // When
        RateLimitService.RateLimitResult result = serviceWithoutRedis.isAllowed(identifier, 10, 15);

        // Then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getLimit()).isEqualTo(10);
        assertThat(result.getRemaining()).isEqualTo(10);
    }

    @Test
    void resetRateLimit_WithRedis_ShouldDeleteKey() {
        // Given
        String identifier = "test_user";
        when(redisTemplate.delete("rate_limit:" + identifier)).thenReturn(true);

        // When
        boolean result = rateLimitService.resetRateLimit(identifier);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).delete("rate_limit:" + identifier);
    }

    @Test
    void resetRateLimit_WithoutRedis_ShouldReturnTrue() {
        // Given
        RateLimitService serviceWithoutRedis = new RateLimitService(null, appProperties);
        String identifier = "test_user";

        // When
        boolean result = serviceWithoutRedis.resetRateLimit(identifier);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void getCurrentStatus_WithoutRedis_ShouldReturnDefaultStatus() {
        // Given
        RateLimitService serviceWithoutRedis = new RateLimitService(null, appProperties);
        String identifier = "test_user";

        // When
        RateLimitService.RateLimitStatus status = serviceWithoutRedis.getCurrentStatus(identifier);

        // Then
        assertThat(status.getLimit()).isEqualTo(100);
        assertThat(status.getRemaining()).isEqualTo(100);
        assertThat(status.getResetTime()).isAfter(Instant.now());
    }

    @Test
    void getCurrentStatus_WithNoStoredValue_ShouldReturnDefaultStatus() {
        // Given
        String identifier = "test_user";
        when(valueOperations.get("rate_limit:" + identifier)).thenReturn(null);

        // When
        RateLimitService.RateLimitStatus status = rateLimitService.getCurrentStatus(identifier);

        // Then
        assertThat(status.getLimit()).isEqualTo(100);
        assertThat(status.getRemaining()).isEqualTo(100);
        assertThat(status.getResetTime()).isAfter(Instant.now());
    }

    @Test
    void rateLimitResult_GettersAndCalculations_ShouldWorkCorrectly() {
        // Given
        Instant resetTime = Instant.now().plusSeconds(3600);
        RateLimitService.RateLimitResult result = new RateLimitService.RateLimitResult(
            true, 100, 50, resetTime);

        // Then
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getLimit()).isEqualTo(100);
        assertThat(result.getRemaining()).isEqualTo(50);
        assertThat(result.getResetTime()).isEqualTo(resetTime);
        assertThat(result.getResetTimeSeconds()).isEqualTo(resetTime.getEpochSecond());
    }

    @Test
    void rateLimitStatus_GettersAndCalculations_ShouldWorkCorrectly() {
        // Given
        Instant resetTime = Instant.now().plusSeconds(1800);
        RateLimitService.RateLimitStatus status = new RateLimitService.RateLimitStatus(
            100, 25, resetTime);

        // Then
        assertThat(status.getLimit()).isEqualTo(100);
        assertThat(status.getRemaining()).isEqualTo(25);
        assertThat(status.getResetTime()).isEqualTo(resetTime);
        assertThat(status.getResetTimeSeconds()).isEqualTo(resetTime.getEpochSecond());
    }
}
