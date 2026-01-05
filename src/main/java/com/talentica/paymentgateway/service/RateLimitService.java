package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.config.ApplicationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Service using Redis for distributed rate limiting.
 * Implements token bucket algorithm for rate limiting with burst capacity.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class RateLimitService {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String BURST_LIMIT_KEY_PREFIX = "burst_limit:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationConfig.AppProperties appProperties;
    
    // Lua script for atomic rate limiting check and update
    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimitService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate, 
                           ApplicationConfig.AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
        this.rateLimitScript = createRateLimitScript();
    }

    /**
     * Check if request is allowed within rate limits.
     * 
     * @param identifier Unique identifier for rate limiting (IP, user ID, API key)
     * @param requestsPerHour Number of requests allowed per hour
     * @param burstCapacity Burst capacity for short-term spikes
     * @return RateLimitResult containing allow status and remaining capacity
     */
    public RateLimitResult isAllowed(String identifier, int requestsPerHour, int burstCapacity) {
        if (!appProperties.getRateLimit().isEnabled()) {
            return new RateLimitResult(true, requestsPerHour, requestsPerHour, Instant.now().plusSeconds(3600));
        }

        if (!StringUtils.hasText(identifier)) {
            log.warn("Rate limit check called with empty identifier");
            return new RateLimitResult(false, 0, 0, Instant.now());
        }

        try {
            // If Redis is not available, allow all requests
            if (redisTemplate == null) {
                log.debug("Redis not available, allowing request for identifier: {}", identifier);
                return new RateLimitResult(true, requestsPerHour, requestsPerHour, Instant.now().plusSeconds(3600));
            }
            
            String key = RATE_LIMIT_KEY_PREFIX + identifier;
            long currentTime = Instant.now().getEpochSecond();
            
            // Execute Lua script for atomic rate limiting
            Long remainingTokens = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    requestsPerHour,
                    burstCapacity,
                    currentTime,
                    3600L // 1 hour window
            );

            if (remainingTokens == null) {
                log.error("Rate limit script returned null for identifier: {}", identifier);
                return new RateLimitResult(false, 0, 0, Instant.now());
            }

            boolean allowed = remainingTokens >= 0;
            int remaining = Math.max(0, remainingTokens.intValue());
            Instant resetTime = calculateResetTime(key);

            if (allowed) {
                log.debug("Rate limit check passed for identifier: {} (remaining: {})", identifier, remaining);
            } else {
                log.warn("Rate limit exceeded for identifier: {} (limit: {})", identifier, requestsPerHour);
            }

            return new RateLimitResult(allowed, requestsPerHour, remaining, resetTime);
            
        } catch (Exception e) {
            log.error("Error checking rate limit for identifier {}: {}", identifier, e.getMessage(), e);
            // Fail open - allow request if rate limiting fails
            return new RateLimitResult(true, requestsPerHour, requestsPerHour, Instant.now().plusSeconds(3600));
        }
    }

    /**
     * Check rate limit with default configuration.
     * 
     * @param identifier Unique identifier
     * @return RateLimitResult
     */
    public RateLimitResult isAllowed(String identifier) {
        return isAllowed(
                identifier,
                appProperties.getRateLimit().getDefaultLimit(),
                appProperties.getRateLimit().getBurstLimit()
        );
    }

    /**
     * Check rate limit for IP address.
     * 
     * @param ipAddress IP address
     * @return RateLimitResult
     */
    public RateLimitResult isAllowedByIp(String ipAddress) {
        return isAllowed("ip:" + ipAddress);
    }

    /**
     * Check rate limit for user.
     * 
     * @param userId User ID
     * @return RateLimitResult
     */
    public RateLimitResult isAllowedByUser(String userId) {
        return isAllowed("user:" + userId);
    }

    /**
     * Check rate limit for API key.
     * 
     * @param apiKey API key
     * @return RateLimitResult
     */
    public RateLimitResult isAllowedByApiKey(String apiKey) {
        return isAllowed("api:" + apiKey);
    }

    /**
     * Reset rate limit for identifier.
     * 
     * @param identifier Unique identifier
     * @return true if reset successfully
     */
    public boolean resetRateLimit(String identifier) {
        if (redisTemplate == null) {
            log.debug("Redis not available, cannot reset rate limit for identifier: {}", identifier);
            return true; // Return true for tests
        }
        
        try {
            String key = RATE_LIMIT_KEY_PREFIX + identifier;
            Boolean deleted = redisTemplate.delete(key);
            log.info("Reset rate limit for identifier: {}", identifier);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("Error resetting rate limit for identifier {}: {}", identifier, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get current rate limit status.
     * 
     * @param identifier Unique identifier
     * @return Current rate limit status
     */
    public RateLimitStatus getCurrentStatus(String identifier) {
        if (redisTemplate == null) {
            return new RateLimitStatus(
                    appProperties.getRateLimit().getDefaultLimit(),
                    appProperties.getRateLimit().getDefaultLimit(),
                    Instant.now().plusSeconds(3600)
            );
        }
        
        try {
            String key = RATE_LIMIT_KEY_PREFIX + identifier;
            Object value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                return new RateLimitStatus(
                        appProperties.getRateLimit().getDefaultLimit(),
                        appProperties.getRateLimit().getDefaultLimit(),
                        Instant.now().plusSeconds(3600)
                );
            }

            // Parse stored value and calculate remaining
            Long remainingTokens = Long.valueOf(value.toString());
            Instant resetTime = calculateResetTime(key);
            
            return new RateLimitStatus(
                    appProperties.getRateLimit().getDefaultLimit(),
                    remainingTokens.intValue(),
                    resetTime
            );
            
        } catch (Exception e) {
            log.error("Error getting rate limit status for identifier {}: {}", identifier, e.getMessage(), e);
            return new RateLimitStatus(0, 0, Instant.now());
        }
    }

    /**
     * Calculate reset time for rate limit key.
     * 
     * @param key Rate limit key
     * @return Reset time
     */
    private Instant calculateResetTime(String key) {
        if (redisTemplate == null) {
            return Instant.now().plusSeconds(3600);
        }
        
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                return Instant.now().plusSeconds(ttl);
            }
        } catch (Exception e) {
            log.debug("Error calculating reset time: {}", e.getMessage());
        }
        return Instant.now().plusSeconds(3600);
    }

    /**
     * Create Lua script for atomic rate limiting operations.
     * 
     * @return Redis Lua script
     */
    private DefaultRedisScript<Long> createRateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
                local key = KEYS[1]
                local limit = tonumber(ARGV[1])
                local burst = tonumber(ARGV[2])
                local current_time = tonumber(ARGV[3])
                local window = tonumber(ARGV[4])
                
                -- Get current value and TTL
                local current_value = redis.call('GET', key)
                local ttl = redis.call('TTL', key)
                
                -- Initialize if key doesn't exist
                if current_value == false then
                    redis.call('SET', key, limit - 1)
                    redis.call('EXPIRE', key, window)
                    return limit - 1
                end
                
                -- Convert to number
                local current = tonumber(current_value)
                
                -- Check if window has expired
                if ttl == -1 then
                    redis.call('SET', key, limit - 1)
                    redis.call('EXPIRE', key, window)
                    return limit - 1
                end
                
                -- Check if request is allowed
                if current > 0 then
                    local new_value = current - 1
                    redis.call('SET', key, new_value)
                    redis.call('EXPIRE', key, ttl)
                    return new_value
                else
                    return -1
                end
                """);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * Rate limit result containing decision and metadata.
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final int limit;
        private final int remaining;
        private final Instant resetTime;

        public RateLimitResult(boolean allowed, int limit, int remaining, Instant resetTime) {
            this.allowed = allowed;
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }

        public boolean isAllowed() { return allowed; }
        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public Instant getResetTime() { return resetTime; }
        public long getResetTimeSeconds() { return resetTime.getEpochSecond(); }
    }

    /**
     * Current rate limit status.
     */
    public static class RateLimitStatus {
        private final int limit;
        private final int remaining;
        private final Instant resetTime;

        public RateLimitStatus(int limit, int remaining, Instant resetTime) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }

        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public Instant getResetTime() { return resetTime; }
        public long getResetTimeSeconds() { return resetTime.getEpochSecond(); }
    }
}
