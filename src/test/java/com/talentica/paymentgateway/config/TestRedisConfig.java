package com.talentica.paymentgateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for Redis-related beans.
 * Provides mock Redis template for testing without requiring actual Redis server.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@TestConfiguration
public class TestRedisConfig {

    /**
     * Mock Redis template for testing.
     * 
     * @return Mock Redis template
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        // Use mock connection factory for tests
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        template.setConnectionFactory(connectionFactory);
        
        // Configure serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        return template;
    }
}
