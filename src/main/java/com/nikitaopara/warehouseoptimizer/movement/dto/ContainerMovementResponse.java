package com.nikitaopara.warehouseoptimizer.movement.dto;

import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovement;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovementType;

import java.time.LocalDateTime;

public record ContainerMovementResponse(
        Long id,
        Long warehouseId,
        ContainerMovementType type,
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
    public static ContainerMovementResponse from(ContainerMovement movement) {
        return new ContainerMovementResponse(
                movement.getId(),
                movement.getWarehouse().getId(),
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
