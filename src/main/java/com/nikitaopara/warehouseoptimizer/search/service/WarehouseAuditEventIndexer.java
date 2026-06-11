package com.nikitaopara.warehouseoptimizer.search.service;

import com.nikitaopara.warehouseoptimizer.eventing.model.DomainEventEnvelope;
import com.nikitaopara.warehouseoptimizer.eventing.model.WarehouseEventTopics;
import com.nikitaopara.warehouseoptimizer.search.model.WarehouseAuditEventDocument;
import com.nikitaopara.warehouseoptimizer.search.repository.WarehouseAuditEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
public class WarehouseAuditEventIndexer {

    private final WarehouseAuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public WarehouseAuditEventIndexer(
            WarehouseAuditEventRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {
                    WarehouseEventTopics.CONTAINER_MOVEMENTS,
                    WarehouseEventTopics.OPTIMIZATION
            },
            groupId = "${app.search.consumer-group:warehouse-audit-indexer}"
    )
    public void index(ConsumerRecord<String, String> record) {
        DomainEventEnvelope envelope = objectMapper.readValue(
                record.value(),
                DomainEventEnvelope.class
        );
        Map<String, Object> payload = objectMapper.convertValue(
                envelope.payload(),
                new TypeReference<>() {
                }
        );
        repository.save(WarehouseAuditEventDocument.from(
                record.topic(),
                record.key(),
                envelope,
                payload
        ));
    }
}
