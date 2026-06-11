package com.nikitaopara.warehouseoptimizer.eventing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false, length = 150)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 150)
    private String eventType;

    @Column(nullable = false, updatable = false, length = 150)
    private String topic;

    @Column(name = "event_key", nullable = false, updatable = false, length = 200)
    private String eventKey;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Version
    @Column(nullable = false)
    private Long version;

    public void claim(Instant leaseExpiresAt) {
        status = OutboxEventStatus.PROCESSING;
        attempts = attempts == null ? 1 : attempts + 1;
        availableAt = leaseExpiresAt;
        lastError = null;
    }

    public void markPublished(Instant publishedAt) {
        status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        availableAt = publishedAt;
        lastError = null;
    }

    public void markFailed(String error, Instant availableAt) {
        status = OutboxEventStatus.FAILED;
        this.availableAt = availableAt;
        this.lastError = truncate(error);
    }

    public Duration calculateRetryDelay(Duration initialDelay, Duration maximumDelay) {
        int retryNumber = Math.max(attempts == null ? 0 : attempts - 1, 0);
        long multiplier = 1L << Math.min(retryNumber, 20);
        Duration calculated = initialDelay.multipliedBy(multiplier);
        return calculated.compareTo(maximumDelay) > 0 ? maximumDelay : calculated;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
