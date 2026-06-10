package com.nikitaopara.warehouseoptimizer.demand.forecast.model;

public record DemandForecastTrainingResult(
        String modelCode,
        int versionNumber,
        DemandForecastModelStatus status,
        int observationCount,
        int articleCount,
        int trainingSampleCount,
        int validationSampleCount,
        DemandForecastMetrics metrics,
        String message
) {
}
