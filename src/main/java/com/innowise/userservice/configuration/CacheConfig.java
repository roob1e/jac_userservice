package com.innowise.userservice.configuration;

import com.innowise.userservice.dto.UserWithCardsResponse;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class CacheConfig {
  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheCustomizer() {
    RedisCacheConfiguration userCache = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                    new JacksonJsonRedisSerializer<>(UserWithCardsResponse.class)
            ));
    return builder -> builder
            .withCacheConfiguration("users", userCache)
            .transactionAware();
  }
}
