package com.nikitaopara.warehouseoptimizer.cache.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@ConditionalOnProperty(
        prefix = "app.cache",
        name = "redis-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class LocalSchedulerLockService implements SchedulerLockService {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public boolean executeWithLock(String lockName, Duration lockAtMost, Runnable task) {
        ReentrantLock lock = locks.computeIfAbsent(lockName, ignored -> new ReentrantLock());

        if (!lock.tryLock()) {
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                locks.remove(lockName, lock);
            }
        }
    }
}
