package com.nikitaopara.warehouseoptimizer.eventing.model;

import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovement;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovementType;

import java.time.LocalDateTime;

public record ContainerMovementEventPayload(
        Long movementId,
        Long warehouseId,
        String warehouseCode,
        ContainerMovementType movementType,
        String containerNumber,
        String articleNumber,
        String targetContainerNumber,
        String fromStoragePlaceCode,
        String toStoragePlaceCode,
        int quantity,
        String optimizationPlanCode,
        Integer relocationStepNumber,
        String performedBy,
        LocalDateTime performedAt
) {
    public static ContainerMovementEventPayload from(ContainerMovement movement) {
        return new ContainerMovementEventPayload(
                movement.getId(),
                movement.getWarehouse().getId(),
                movement.getWarehouse().getCode(),
                movement.getType(),
                movement.getContainerNumber(),
                movement.getArticleNumber(),
                movement.getTargetContainerNumber(),
                movement.getFromStoragePlaceCode(),
                movement.getToStoragePlaceCode(),
                movement.getQuantity(),
                movement.getOptimizationPlan() == null
                        ? null
                        : movement.getOptimizationPlan().getCode(),
                movement.getRelocationStep() == null
                        ? null
                        : movement.getRelocationStep().getSequenceNumber(),
                movement.getPerformedBy().getEmail(),
                movement.getPerformedAt()
        );
    }
}
