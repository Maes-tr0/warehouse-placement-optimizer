package com.nikitaopara.warehouseoptimizer.demand.dto;

public record ImportDemandOrderItemRequest(
        String articleNumber,
        Integer quantity
) {
}