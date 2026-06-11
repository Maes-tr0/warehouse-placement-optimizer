package com.nikitaopara.warehouseoptimizer.demand.forecast.model;

public record TrainedDemandForecast(
        byte[] artifact,
        DemandForecastMetrics metrics
) {
}
