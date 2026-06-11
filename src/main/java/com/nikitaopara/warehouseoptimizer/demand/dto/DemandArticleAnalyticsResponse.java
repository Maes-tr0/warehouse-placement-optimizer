package com.nikitaopara.warehouseoptimizer.demand.dto;

import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandScoreSource;

import java.math.BigDecimal;

public record DemandArticleAnalyticsResponse(
        Integer rank,
        Long articleId,
        String articleNumber,
        String articleName,
        double demandScore,
        DemandScoreSource scoreSource,
        String modelCode,
        Integer forecastHorizonDays,
        long historicalQuantity,
        long orderCount,
        long storedQuantity,
        long storedContainerCount,
        BigDecimal averageDistanceFromEntryMm,
        String explanation
) {
}
