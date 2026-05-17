package com.yuno.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for:
 * - Idempotency key storage (TTL-based)
 * - Provider health status caching
 * 
 * When Redis is not available (dev mode), the IdempotencyService
 * falls back to a no-op implementation that relies on
 * PostgreSQL unique constraints as the safety net.
 */
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = false)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
            return template;
        } catch (Exception e) {
            // Return null — IdempotencyService handles null gracefully
            return null;
        }
    }
}
