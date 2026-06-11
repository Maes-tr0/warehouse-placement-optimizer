package com.nikitaopara.warehouseoptimizer.eventing.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    void capsExponentialRetryDelay() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .status(OutboxEventStatus.PENDING)
                .attempts(8)
                .availableAt(Instant.now())
                .build();

        assertThat(event.calculateRetryDelay(
                Duration.ofSeconds(10),
                Duration.ofMinutes(15)
        )).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void claimCreatesProcessingLeaseAndIncrementsAttempt() {
        Instant leaseExpiresAt = Instant.parse("2026-06-11T05:00:00Z");
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .availableAt(Instant.now())
                .build();

        event.claim(leaseExpiresAt);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getAvailableAt()).isEqualTo(leaseExpiresAt);
    }
}
