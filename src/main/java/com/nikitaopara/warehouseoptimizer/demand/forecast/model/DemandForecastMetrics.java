package com.nikitaopara.warehouseoptimizer.demand.forecast.model;

public record DemandForecastMetrics(
        double modelMae,
        double baselineMae,
        double modelRmse,
        double modelR2,
        double improvementPercent
) {
}
