package com.nikitaopara.warehouseoptimizer.eventing.model;

import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        Object payload
) {
}
