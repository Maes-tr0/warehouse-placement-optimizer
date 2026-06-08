package com.nikitaopara.warehouseoptimizer.warehouse.dto;

public record CreateRackLevelProfileRequest(
        Integer levelNumber,
        Integer clearHeightMm,
        Integer maxCellLoadKg
) {
}