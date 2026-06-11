package com.nikitaopara.warehouseoptimizer.eventing.service;

import com.nikitaopara.warehouseoptimizer.eventing.model.DomainEventEnvelope;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEvent;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEventStatus;
import com.nikitaopara.warehouseoptimizer.eventing.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public UUID publish(
            String topic,
            String eventType,
            String aggregateType,
            String aggregateId,
            String eventKey,
            Object payload
    ) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                eventType,
                aggregateType,
                aggregateId,
                occurredAt,
                payload
        );
        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .aggregateType(requireText(aggregateType, "aggregateType"))
                .aggregateId(requireText(aggregateId, "aggregateId"))
                .eventType(requireText(eventType, "eventType"))
                .topic(requireText(topic, "topic"))
                .eventKey(requireText(eventKey, "eventKey"))
                .payload(serialize(envelope))
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .availableAt(occurredAt)
                .occurredAt(occurredAt)
                .build();

        outboxEventRepository.save(event);
        return eventId;
    }

    private String serialize(DomainEventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Domain event payload cannot be serialized", exception);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
