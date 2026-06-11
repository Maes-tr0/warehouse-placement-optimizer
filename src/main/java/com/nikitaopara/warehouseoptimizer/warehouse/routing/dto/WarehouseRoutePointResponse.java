package com.nikitaopara.warehouseoptimizer.warehouse.routing.dto;

import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRouteNode;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRouteNodeType;

public record WarehouseRoutePointResponse(
        WarehouseRouteNodeType type,
        String label,
        int xMm,
        int yMm
) {
    public static WarehouseRoutePointResponse from(WarehouseRouteNode node) {
        return new WarehouseRoutePointResponse(
                node.type(),
                node.label(),
                node.xMm(),
                node.yMm()
        );
    }
}
