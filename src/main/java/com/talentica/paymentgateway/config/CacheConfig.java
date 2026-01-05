package com.talentica.paymentgateway.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.talentica.paymentgateway.constants.CacheConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache configuration with Redis backend.
 * Provides centralized cache management with TTL settings for different cache regions.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.features.redis-cache", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    /**
     * Configure Redis-based cache manager with custom TTL settings.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create ObjectMapper with polymorphic type handling for security
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // Configure JSON serializer
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Default cache configuration (1 hour TTL)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        // Custom cache configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // API Key validation cache - 1 hour
        cacheConfigurations.put(CacheConstants.API_KEY_VALIDATION_CACHE, 
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.API_KEY_CACHE_TTL)));
        
        // Client permissions cache - 1 hour
        cacheConfigurations.put(CacheConstants.CLIENT_PERMISSIONS_CACHE, 
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.PERMISSIONS_CACHE_TTL)));
        
        // Customer profile cache - 30 minutes
        cacheConfigurations.put(CacheConstants.CUSTOMER_PROFILES_CACHE, 
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.CUSTOMER_CACHE_TTL)));
        
        // Subscription plans cache - 6 hours (rarely changes)
        cacheConfigurations.put(CacheConstants.SUBSCRIPTION_PLANS_CACHE, 
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.SUBSCRIPTION_PLAN_CACHE_TTL)));
        
        // Payment methods cache - 15 minutes
        cacheConfigurations.put(CacheConstants.PAYMENT_METHODS_CACHE, 
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.PAYMENT_METHOD_CACHE_TTL)));
        
        // User details cache - 30 minutes
        cacheConfigurations.put(CacheConstants.USER_DETAILS_CACHE, 
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.USER_CACHE_TTL)));
        
        // Analytics data cache - 5 minutes
        cacheConfigurations.put(CacheConstants.ANALYTICS_CACHE, 
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.ANALYTICS_CACHE_TTL)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
