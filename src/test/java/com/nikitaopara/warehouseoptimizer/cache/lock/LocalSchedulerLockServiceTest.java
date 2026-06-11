package com.nikitaopara.warehouseoptimizer.cache.lock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LocalSchedulerLockServiceTest {

    @Test
    void preventsConcurrentExecutionForSameLockName() throws Exception {
        LocalSchedulerLockService service = new LocalSchedulerLockService();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean firstResult = new AtomicBoolean();

        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> firstResult.set(service.executeWithLock(
                    "warehouse-1",
                    Duration.ofMinutes(1),
                    () -> {
                        entered.countDown();
                        await(release);
                    }
            )));
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();

            boolean secondResult = service.executeWithLock(
                    "warehouse-1",
                    Duration.ofMinutes(1),
                    () -> {
                    }
            );

            assertThat(secondResult).isFalse();
            release.countDown();
            future.get(1, TimeUnit.SECONDS);
            assertThat(firstResult).isTrue();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
