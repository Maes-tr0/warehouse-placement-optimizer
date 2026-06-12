package com.nikitaopara.warehouseoptimizer.cache.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "redis-enabled", havingValue = "true")
    CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            CacheProperties properties
    ) {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.nikitaopara.warehouseoptimizer.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .build();

        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .typePropertyName("@class")
                .enableDefaultTyping(typeValidator)
                .enableSpringCacheNullValueSupport("@class")
                .build();

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.getTimeToLive())
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        serializer
                ));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager(
                CacheNames.WAREHOUSES,
                CacheNames.WAREHOUSE_ROUTES,
                CacheNames.DEMAND_ANALYTICS,
                CacheNames.LATEST_ASSESSMENTS
        );
    }
}