package com.auth.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		// Replace with your Redis host and port if needed
		return new LettuceConnectionFactory("localhost", 6379);
	}

	@Bean
	public StringRedisTemplate redisTemplate(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}

}
