package com.nikitaopara.warehouseoptimizer.cache.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.cache", name = "redis-enabled", havingValue = "true")
public class RedisSchedulerLockService implements SchedulerLockService {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisSchedulerLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean executeWithLock(String lockName, Duration lockAtMost, Runnable task) {
        String key = "warehouse-optimizer:locks:" + lockName;
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, lockAtMost);

        if (!Boolean.TRUE.equals(acquired)) {
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(key), token);
        }
    }
}
