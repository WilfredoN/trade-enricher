package com.capybara.trade.enricher.config;

import com.capybara.trade.enricher.model.Product;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Product> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        RedisSerializationContext<String, Product> context = RedisSerializationContext
                .<String, Product>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
