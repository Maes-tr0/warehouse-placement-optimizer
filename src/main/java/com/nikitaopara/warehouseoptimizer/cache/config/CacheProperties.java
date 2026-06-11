package com.nikitaopara.warehouseoptimizer.cache.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.cache")
@Getter
@Setter
public class CacheProperties {

    private boolean redisEnabled;
    private Duration timeToLive = Duration.ofMinutes(5);
    private Duration schedulerLockAtMost = Duration.ofMinutes(30);
}
