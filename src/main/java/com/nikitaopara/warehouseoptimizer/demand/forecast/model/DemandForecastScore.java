package com.nikitaopara.warehouseoptimizer.demand.forecast.model;

import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;

public record DemandForecastScore(
        ArticleDemandScore score,
        DemandScoreSource source,
        String modelCode,
        Integer forecastHorizonDays
) {
}
