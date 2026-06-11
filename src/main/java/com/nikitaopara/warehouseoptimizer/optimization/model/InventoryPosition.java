package com.nikitaopara.warehouseoptimizer.optimization.model;

public record InventoryPosition(
        Long containerId,
        Long articleId,
        int quantity,
        int distanceFromEntryMm
) {
}
