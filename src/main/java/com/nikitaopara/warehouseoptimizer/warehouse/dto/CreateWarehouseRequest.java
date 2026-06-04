package com.nikitaopara.warehouseoptimizer.warehouse.dto;

import java.util.List;

public record CreateWarehouseRequest(
        String warehouseCode
        ,String warehouseName
        ,Integer rackRowCount
        ,Integer baysPerRackRow
        ,Integer positionsPerLevel
        ,Integer aisleWidthMm
        ,Integer maxBayLoadKg
        ,List<CreateRackLevelProfileRequest> levelProfiles
) {
}