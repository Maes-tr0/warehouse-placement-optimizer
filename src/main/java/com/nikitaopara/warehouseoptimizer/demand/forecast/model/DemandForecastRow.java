package com.nikitaopara.warehouseoptimizer.demand.forecast.model;

import java.time.LocalDate;

public record DemandForecastRow(
        Long articleId,
        LocalDate featureDate,
        double targetQuantity,
        double baselineQuantity,
        String[] featureNames,
        double[] featureValues
) {
}
