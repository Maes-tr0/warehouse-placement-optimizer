package com.nikitaopara.warehouseoptimizer.optimization.model;

import java.math.BigDecimal;

public record WarehouseEfficiencyResult(
        BigDecimal scorePercent,
        BigDecimal weightedAverageDistanceMm,
        int demandMatchedContainers
) {
}
