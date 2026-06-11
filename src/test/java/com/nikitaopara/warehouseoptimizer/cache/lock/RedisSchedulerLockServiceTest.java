package com.nikitaopara.warehouseoptimizer.cache.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class RedisSchedulerLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void executesAndReleasesOnlyAnAcquiredLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("warehouse-optimizer:locks:training:1"),
                anyString(),
                eq(Duration.ofMinutes(5))
        )).thenReturn(true);
        RedisSchedulerLockService service = new RedisSchedulerLockService(redisTemplate);
        AtomicBoolean executed = new AtomicBoolean();

        boolean acquired = service.executeWithLock(
                "training:1",
                Duration.ofMinutes(5),
                () -> executed.set(true)
        );

        assertThat(acquired).isTrue();
        assertThat(executed).isTrue();
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("warehouse-optimizer:locks:training:1")),
                anyString()
        );
    }
}
