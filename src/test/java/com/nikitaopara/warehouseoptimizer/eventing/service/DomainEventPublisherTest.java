package com.nikitaopara.warehouseoptimizer.eventing.service;

import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEvent;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEventStatus;
import com.nikitaopara.warehouseoptimizer.eventing.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private OutboxEventRepository repository;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void persistsSerializedEnvelopeAsPendingEvent() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"test.created\"}");
        DomainEventPublisher publisher = new DomainEventPublisher(repository, objectMapper);

        var eventId = publisher.publish(
                "warehouse.test.v1",
                "test.created",
                "TestAggregate",
                "42",
                "WH-1",
                java.util.Map.of("value", 10)
        );

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getId()).isEqualTo(eventId);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getEventKey()).isEqualTo("WH-1");
        assertThat(event.getPayload()).contains("test.created");
    }
}
