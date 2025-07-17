package com.redis.triage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * Configuration for Redis connections
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    /**
     * Creates a JedisPooled bean for Redis operations
     * 
     * @return Configured JedisPooled instance
     */
    @Bean
    public JedisPooled jedisPooled() {
        if (redisPassword != null && !redisPassword.isEmpty()) {
            return new JedisPooled(redisHost, redisPort, null, redisPassword);
        } else {
            return new JedisPooled(redisHost, redisPort);
        }
    }
}
