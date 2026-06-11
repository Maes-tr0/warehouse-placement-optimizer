package com.nikitaopara.warehouseoptimizer.eventing.service;

import com.nikitaopara.warehouseoptimizer.eventing.config.EventingProperties;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEvent;
import com.nikitaopara.warehouseoptimizer.observability.WarehouseBusinessMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxDispatcherTest {

    @Mock
    private OutboxEventDeliveryService deliveryService;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private WarehouseBusinessMetrics businessMetrics;

    @Test
    void marksEventPublishedAfterKafkaAcknowledgement() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .topic("warehouse.test.v1")
                .eventKey("WH-1")
                .payload("{\"eventId\":\"" + eventId + "\"}")
                .attempts(1)
                .build();
        when(deliveryService.claimBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(null));
        KafkaOutboxDispatcher dispatcher = new KafkaOutboxDispatcher(
                deliveryService,
                kafkaTemplate,
                new EventingProperties(),
                businessMetrics
        );

        dispatcher.dispatchPendingEvents();

        verify(deliveryService).markPublished(eventId);
        verify(businessMetrics).outboxDelivery(event, "published");
    }
}
