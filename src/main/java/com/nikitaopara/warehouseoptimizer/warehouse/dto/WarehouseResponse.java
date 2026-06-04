package com.nikitaopara.warehouseoptimizer.warehouse.dto;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseType;

import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id
        ,String warehouseCode
        ,String warehouseName
        ,WarehouseType type
        ,WarehouseStatus status
        ,Integer generatedAisleCount
        ,Integer generatedRackRowCount
        ,Integer generatedRackBayCount
        ,Integer generatedRackLevelCount
        ,Integer generatedStoragePlaceCount
        ,LocalDateTime createdAt
        ,LocalDateTime updatedAt
) {

    public static WarehouseResponse from(
            Warehouse warehouse,
            Integer generatedAisleCount,
            Integer generatedRackRowCount,
            Integer generatedRackBayCount,
            Integer generatedRackLevelCount,
            Integer generatedStoragePlaceCount
    ) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getType(),
                warehouse.getStatus(),
                generatedAisleCount,
                generatedRackRowCount,
                generatedRackBayCount,
                generatedRackLevelCount,
                generatedStoragePlaceCount,
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }
}