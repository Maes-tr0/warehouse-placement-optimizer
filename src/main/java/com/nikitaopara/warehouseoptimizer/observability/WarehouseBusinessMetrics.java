package com.nikitaopara.warehouseoptimizer.observability;

import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEvent;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovement;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationAssessment;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarehouseBusinessMetrics {

    private final MeterRegistry meterRegistry;

    public void containerMovementRecorded(ContainerMovement movement) {
        meterRegistry.counter(
                "warehouse.container.movements",
                "type",
                movement.getType().name()
        ).increment();
    }

    public void optimizationAssessed(WarehouseOptimizationAssessment assessment) {
        meterRegistry.counter(
                "warehouse.optimization.assessments",
                "status",
                assessment.getStatus().name(),
                "trigger",
                assessment.getTrigger().name()
        ).increment();
    }

    public void outboxDelivery(OutboxEvent event, String result) {
        meterRegistry.counter(
                "warehouse.outbox.deliveries",
                "eventType",
                event.getEventType(),
                "result",
                result
        ).increment();
    }
}
