package com.nikitaopara.warehouseoptimizer.optimization.dto;

import com.nikitaopara.warehouseoptimizer.optimization.model.RelocationStepStatus;
import com.nikitaopara.warehouseoptimizer.optimization.model.RelocationStepType;

import java.time.LocalDateTime;

public record RelocationStepResponse(
        Long id,
        int sequenceNumber,
        RelocationStepType type,
        RelocationStepStatus status,
        String sourceContainerNumber,
        String sourceArticleNumber,
        String targetContainerNumber,
        String targetArticleNumber,
        String fromStoragePlaceCode,
        String toStoragePlaceCode,
        long estimatedTimeSavingSeconds,
        String reason,
        LocalDateTime completedAt
) {
}
