package com.nikitaopara.warehouseoptimizer.cache.lock;

import java.time.Duration;

public interface SchedulerLockService {

    boolean executeWithLock(String lockName, Duration lockAtMost, Runnable task);
}
