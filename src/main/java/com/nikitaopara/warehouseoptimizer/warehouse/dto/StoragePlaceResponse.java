package com.nikitaopara.warehouseoptimizer.warehouse.dto;

import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;

public record StoragePlaceResponse(
        Long id,
        Long warehouseId,
        String code,
        String rackRowCode,
        String rackBayCode,
        String rackLevelCode,
        Integer levelNumber,
        Integer positionNumber,
        Integer maxWeightKg,
        Integer maxHeightMm,
        Integer accessXMm,
        Integer accessYMm,
        Integer distanceFromEntryMm,
        StoragePlaceStatus status
) {

    public static StoragePlaceResponse from(StoragePlace place) {
        return new StoragePlaceResponse(
                place.getId(),
                place.getWarehouse().getId(),
                place.getCode(),
                place.getRackRow().getCode(),
                place.getRackBay().getCode(),
                place.getRackLevel().getCode(),
                place.getRackLevel().getLevelNumber(),
                place.getPositionNumber(),
                place.getMaxWeightKg(),
                place.getMaxHeightMm(),
                place.getAccessXMm(),
                place.getAccessYMm(),
                place.getDistanceFromEntryMm(),
                place.getStatus()
        );
    }
}
