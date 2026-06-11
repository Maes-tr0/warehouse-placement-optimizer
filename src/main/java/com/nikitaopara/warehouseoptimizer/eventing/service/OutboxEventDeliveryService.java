package com.nikitaopara.warehouseoptimizer.eventing.service;

import com.nikitaopara.warehouseoptimizer.eventing.config.EventingProperties;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEvent;
import com.nikitaopara.warehouseoptimizer.eventing.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventDeliveryService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventingProperties properties;

    @Transactional
    public List<OutboxEvent> claimBatch() {
        Instant now = Instant.now();
        outboxEventRepository.recoverExpiredProcessingLeases(now);
        List<OutboxEvent> events = outboxEventRepository.findDispatchableForUpdate(
                now,
                properties.getDispatchBatchSize()
        );
        Instant leaseExpiresAt = now.plus(properties.getProcessingLease());
        events.forEach(event -> event.claim(leaseExpiresAt));

        return List.copyOf(events);
    }

    @Transactional
    public void markPublished(UUID eventId) {
        OutboxEvent event = getEvent(eventId);
        event.markPublished(Instant.now());
    }

    @Transactional
    public void markFailed(UUID eventId, Throwable failure) {
        OutboxEvent event = getEvent(eventId);
        Duration retryDelay = event.calculateRetryDelay(
                properties.getInitialRetryDelay(),
                properties.getMaximumRetryDelay()
        );
        event.markFailed(failureMessage(failure), Instant.now().plus(retryDelay));
    }

    private OutboxEvent getEvent(UUID eventId) {
        return outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event not found: " + eventId
                ));
    }

    private String failureMessage(Throwable failure) {
        if (failure == null) {
            return "Unknown Kafka delivery failure";
        }
        if (failure.getMessage() == null || failure.getMessage().isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }
}
