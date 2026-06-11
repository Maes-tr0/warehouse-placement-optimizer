package com.nikitaopara.warehouseoptimizer.notification.service;

import com.nikitaopara.warehouseoptimizer.eventing.model.DomainEventEnvelope;
import com.nikitaopara.warehouseoptimizer.eventing.model.OptimizationAssessmentEventPayload;
import com.nikitaopara.warehouseoptimizer.eventing.model.WarehouseEventTopics;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(
        prefix = "app.notifications",
        name = "email-enabled",
        havingValue = "true"
)
public class OptimizationEmailEventListener {

    private final OptimizationEmailNotificationService notificationService;
    private final ObjectMapper objectMapper;

    public OptimizationEmailEventListener(
            OptimizationEmailNotificationService notificationService,
            ObjectMapper objectMapper
    ) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = WarehouseEventTopics.OPTIMIZATION,
            groupId = "${app.notifications.consumer-group:warehouse-optimization-email}"
    )
    public void onOptimizationAssessment(ConsumerRecord<String, String> record) {
        DomainEventEnvelope envelope = objectMapper.readValue(
                record.value(),
                DomainEventEnvelope.class
        );
        if (!"warehouse.optimization.assessed".equals(envelope.eventType())) {
            return;
        }

        OptimizationAssessmentEventPayload assessment = objectMapper.convertValue(
                envelope.payload(),
                OptimizationAssessmentEventPayload.class
        );
        if (assessment.status() == OptimizationAssessmentStatus.OPTIMIZATION_RECOMMENDED) {
            notificationService.sendOptimizationRecommended(assessment);
        }
    }
}
