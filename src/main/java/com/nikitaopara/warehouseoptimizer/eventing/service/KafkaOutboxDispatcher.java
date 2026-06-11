package com.nikitaopara.warehouseoptimizer.eventing.service;

import com.nikitaopara.warehouseoptimizer.eventing.config.EventingProperties;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEvent;
import com.nikitaopara.warehouseoptimizer.observability.WarehouseBusinessMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "app.events", name = "kafka-enabled", havingValue = "true")
public class KafkaOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxDispatcher.class);

    private final OutboxEventDeliveryService deliveryService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventingProperties properties;
    private final WarehouseBusinessMetrics businessMetrics;

    public KafkaOutboxDispatcher(
            OutboxEventDeliveryService deliveryService,
            KafkaTemplate<String, String> kafkaTemplate,
            EventingProperties properties,
            WarehouseBusinessMetrics businessMetrics
    ) {
        this.deliveryService = deliveryService;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.businessMetrics = businessMetrics;
    }

    @Scheduled(fixedDelayString = "${app.events.dispatch-interval:2s}")
    public void dispatchPendingEvents() {
        for (OutboxEvent event : deliveryService.claimBatch()) {
            dispatch(event);
        }
    }

    private void dispatch(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                    .get(properties.getSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            deliveryService.markPublished(event.getId());
            businessMetrics.outboxDelivery(event, "published");
        } catch (Exception exception) {
            deliveryService.markFailed(event.getId(), exception);
            businessMetrics.outboxDelivery(event, "failed");
            log.warn(
                    "Kafka delivery failed for outbox event {} on attempt {}",
                    event.getId(),
                    event.getAttempts(),
                    exception
            );
        }
    }
}
