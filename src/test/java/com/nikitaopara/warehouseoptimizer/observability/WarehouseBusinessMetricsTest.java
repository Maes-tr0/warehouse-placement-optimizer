package com.nikitaopara.warehouseoptimizer.observability;

import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovement;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovementType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseBusinessMetricsTest {

    @Test
    void recordsMovementCounterWithBusinessTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WarehouseBusinessMetrics metrics = new WarehouseBusinessMetrics(registry);

        metrics.containerMovementRecorded(ContainerMovement.builder()
                .type(ContainerMovementType.RELOCATION)
                .build());

        assertThat(registry.get("warehouse.container.movements")
                .tag("type", "RELOCATION")
                .counter()
                .count()).isEqualTo(1.0);
    }
}
