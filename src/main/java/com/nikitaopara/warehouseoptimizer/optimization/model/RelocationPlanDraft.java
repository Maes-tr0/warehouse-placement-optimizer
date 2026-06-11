package com.nikitaopara.warehouseoptimizer.optimization.model;

import java.math.BigDecimal;
import java.util.List;

public record RelocationPlanDraft(
        List<PlannedRelocationStep> steps,
        BigDecimal projectedScorePercent,
        long estimatedTimeSavingSeconds
) {
}
