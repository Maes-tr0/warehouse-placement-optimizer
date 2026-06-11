package com.nikitaopara.warehouseoptimizer.optimization.model;

public record PlannedRelocationStep(
        RelocationStepType type,
        Long sourceContainerId,
        Long targetContainerId,
        Long fromStoragePlaceId,
        Long toStoragePlaceId,
        long estimatedTimeSavingSeconds,
        String reason
) {
}
