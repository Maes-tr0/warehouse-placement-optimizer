package com.nikitaopara.warehouseoptimizer.warehouse.routing.dto;

import java.util.List;

public record WarehouseRouteResponse(
        Long warehouseId,
        String storagePlaceCode,
        int distanceMm,
        int estimatedTravelTimeSeconds,
        List<WarehouseRoutePointResponse> points
) {
    public WarehouseRouteResponse {
        points = List.copyOf(points);
    }
}
