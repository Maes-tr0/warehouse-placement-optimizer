package com.nikitaopara.warehouseoptimizer.warehouse.dto;

public record CreateRackLevelProfileRequest(
        Integer levelNumber
        ,Integer cellHeightMm
        ,Integer maxCellLoadKg
) {
}
