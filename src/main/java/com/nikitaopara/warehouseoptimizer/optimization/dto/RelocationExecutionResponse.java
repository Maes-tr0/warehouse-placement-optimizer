package com.nikitaopara.warehouseoptimizer.optimization.dto;

import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationPlanStatus;

public record RelocationExecutionResponse(
        String planCode,
        OptimizationPlanStatus planStatus,
        RelocationStepResponse completedStep,
        RelocationStepResponse nextStep
) {
}
