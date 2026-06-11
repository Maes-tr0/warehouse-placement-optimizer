package com.nikitaopara.warehouseoptimizer.eventing.model;

import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentStatus;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentTrigger;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationAssessment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OptimizationAssessmentEventPayload(
        Long assessmentId,
        Long warehouseId,
        String warehouseCode,
        OptimizationAssessmentStatus status,
        OptimizationAssessmentTrigger trigger,
        BigDecimal scorePercent,
        BigDecimal thresholdPercent,
        BigDecimal weightedAverageDistanceMm,
        int demandObservationCount,
        int analyzedContainerCount,
        LocalDateTime analyzedAt
) {
    public static OptimizationAssessmentEventPayload from(
            WarehouseOptimizationAssessment assessment
    ) {
        return new OptimizationAssessmentEventPayload(
                assessment.getId(),
                assessment.getWarehouse().getId(),
                assessment.getWarehouse().getCode(),
                assessment.getStatus(),
                assessment.getTrigger(),
                assessment.getScorePercent(),
                assessment.getThresholdPercent(),
                assessment.getWeightedAverageDistanceMm(),
                assessment.getDemandObservationCount(),
                assessment.getAnalyzedContainerCount(),
                assessment.getAnalyzedAt()
        );
    }
}
