package com.nikitaopara.warehouseoptimizer.warehouse.routing.model;

public record WarehouseRouteNode(
        String id,
        WarehouseRouteNodeType type,
        String label,
        int xMm,
        int yMm
) {
}
