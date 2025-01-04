package com.example.student.config;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;

import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@EnableRetry
public class RedisConfig {

    private static final Logger logger = Logger.getLogger(RedisConfig.class.getName());

    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @CircuitBreaker(name = "redisService", fallbackMethod = "fallbackForRedis")
    public LettuceConnectionFactory redisConnectionFactory() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
            return new LettuceConnectionFactory(config);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error while creating Redis connection factory", ex);
            throw ex;
        }
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    public LettuceConnectionFactory fallbackForRedis(Exception ex) {
        logger.log(Level.SEVERE, "Failed to connect to Redis. Circuit breaker is open. Falling back to default connection.", ex);
        return getDefaultLettuceConnectionFactory();
    }
    private LettuceConnectionFactory getDefaultLettuceConnectionFactory() {
        RedisStandaloneConfiguration defaultConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        logger.info("Fallback: Returning default Redis connection");
        return new LettuceConnectionFactory(defaultConfig);
    }
}
