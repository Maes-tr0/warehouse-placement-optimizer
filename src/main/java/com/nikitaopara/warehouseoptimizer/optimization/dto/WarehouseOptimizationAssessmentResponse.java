package com.nikitaopara.warehouseoptimizer.optimization.dto;

import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentStatus;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentTrigger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WarehouseOptimizationAssessmentResponse(
        Long id,
        Long warehouseId,
        OptimizationAssessmentStatus status,
        OptimizationAssessmentTrigger trigger,
        boolean optimizationRecommended,
        BigDecimal scorePercent,
        BigDecimal thresholdPercent,
        BigDecimal weightedAverageDistanceMm,
        LocalDateTime lookbackStart,
        LocalDateTime analyzedAt,
        int demandObservationCount,
        int analyzedContainerCount,
        int demandMatchedContainerCount
) {
}
