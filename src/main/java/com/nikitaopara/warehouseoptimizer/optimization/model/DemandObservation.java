package com.nikitaopara.warehouseoptimizer.optimization.model;

import java.time.LocalDateTime;

public record DemandObservation(
        Long articleId,
        Long orderId,
        LocalDateTime orderedAt,
        int quantity
) {
}
