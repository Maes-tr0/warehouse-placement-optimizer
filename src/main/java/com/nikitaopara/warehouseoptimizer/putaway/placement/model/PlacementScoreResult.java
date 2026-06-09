package com.nikitaopara.warehouseoptimizer.putaway.placement.model;

import java.math.BigDecimal;

public record PlacementScoreResult(
        Integer distanceFromEntryMm,
        Integer estimatedTimeSeconds,
        BigDecimal score,
        String reason
) {
}