package com.nikitaopara.warehouseoptimizer.demand.forecast.model;

import java.time.LocalDate;

public record DailyArticleDemand(
        Long articleId,
        LocalDate date,
        int quantity,
        int orderCount
) {
}
