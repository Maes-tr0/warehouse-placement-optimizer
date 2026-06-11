package com.nikitaopara.warehouseoptimizer.optimization.dto;

import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationPlanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record WarehouseOptimizationPlanResponse(
        String code,
        Long warehouseId,
        Long assessmentId,
        OptimizationPlanStatus status,
        BigDecimal initialScorePercent,
        BigDecimal targetScorePercent,
        BigDecimal projectedScorePercent,
        long estimatedTimeSavingSeconds,
        int totalSteps,
        int completedSteps,
        LocalDateTime approvedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        List<RelocationStepResponse> steps
) {
}
