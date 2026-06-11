package com.nikitaopara.warehouseoptimizer.optimization.model;

public record ArticleDemandScore(
        Long articleId,
        double weightedDemand,
        long totalQuantity,
        long orderCount
) {
}
