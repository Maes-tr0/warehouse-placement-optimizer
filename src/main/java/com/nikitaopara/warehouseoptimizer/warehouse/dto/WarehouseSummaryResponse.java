package com.nikitaopara.warehouseoptimizer.warehouse.dto;

import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseLayoutType;
import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseStatus;

import java.time.LocalDateTime;

public record WarehouseSummaryResponse(
        Long id,
        String warehouseCode,
        String warehouseName,
        WarehouseLayoutType layoutType,
        WarehouseStatus status,
        Long aisleCount,
        Long rackRowCount,
        Long rackBayCount,
        Long rackLevelCount,
        Long storagePlaceCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
