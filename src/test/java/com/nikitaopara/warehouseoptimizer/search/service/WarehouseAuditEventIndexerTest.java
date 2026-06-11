package com.nikitaopara.warehouseoptimizer.search.service;

import com.nikitaopara.warehouseoptimizer.eventing.model.DomainEventEnvelope;
import com.nikitaopara.warehouseoptimizer.search.model.WarehouseAuditEventDocument;
import com.nikitaopara.warehouseoptimizer.search.repository.WarehouseAuditEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class WarehouseAuditEventIndexerTest {

    @Mock
    private WarehouseAuditEventRepository repository;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void indexesEnvelopeUsingEventIdAsDocumentId() throws Exception {
        UUID eventId = UUID.randomUUID();
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                "container.movement.recorded",
                "ContainerMovement",
                "41",
                Instant.parse("2026-06-11T03:00:00Z"),
                Map.of("containerNumber", "C-1")
        );
        when(objectMapper.readValue("payload", DomainEventEnvelope.class)).thenReturn(envelope);
        when(objectMapper.convertValue(eq(envelope.payload()), any(TypeReference.class)))
                .thenReturn(Map.of("containerNumber", "C-1"));
        WarehouseAuditEventIndexer indexer = new WarehouseAuditEventIndexer(
                repository,
                objectMapper
        );
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "warehouse.container-movements.v1",
                0,
                0,
                "WH-1",
                "payload"
        );

        indexer.index(record);

        ArgumentCaptor<WarehouseAuditEventDocument> captor = ArgumentCaptor.forClass(
                WarehouseAuditEventDocument.class
        );
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(eventId.toString());
        assertThat(captor.getValue().warehouseKey()).isEqualTo("WH-1");
        assertThat(captor.getValue().payload()).containsEntry("containerNumber", "C-1");
    }
}
