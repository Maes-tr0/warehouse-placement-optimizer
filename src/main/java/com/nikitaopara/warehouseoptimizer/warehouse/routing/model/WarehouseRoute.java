package com.nikitaopara.warehouseoptimizer.warehouse.routing.model;

import java.util.List;

public record WarehouseRoute(
        List<WarehouseRouteNode> nodes,
        int distanceMm
) {
    public WarehouseRoute {
        nodes = List.copyOf(nodes);
    }
}
