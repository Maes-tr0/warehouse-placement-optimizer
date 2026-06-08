package com.nikitaopara.warehouseoptimizer.warehouse.dto;

import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseLayoutType;

import java.util.List;

public record CreateWarehouseRequest(
        String warehouseCode,
        String warehouseName,
        WarehouseLayoutType layoutType,
        Integer aisleCount,
        Integer rackRowCount,
        Integer baysPerRackRow,
        Integer palletPlacesPerLevel,
        Integer aisleWidthMm,
        List<CreateRackLevelProfileRequest> levelProfiles,
        Integer maxBayLoadKg
) {
}