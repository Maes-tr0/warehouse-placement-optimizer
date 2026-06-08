package com.nikitaopara.warehouseoptimizer.warehouse.dto;

import com.nikitaopara.warehouseoptimizer.warehouse.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record WarehouseResponse(
        Long id,
        String warehouseCode,
        String warehouseName,
        WarehouseLayoutType layoutType,
        WarehouseStatus status,
        Integer generatedAisleCount,
        Integer generatedRackRowCount,
        Integer generatedRackBayCount,
        Integer generatedRackLevelCount,
        Integer generatedStoragePlaceCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getLayoutType(),
                warehouse.getStatus(),
                countAisles(warehouse),
                countRackRows(warehouse),
                countRackBays(warehouse),
                countRackLevels(warehouse),
                countStoragePlaces(warehouse),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }

    private static Integer countAisles(Warehouse warehouse) {
        if (warehouse.getAisles() == null) {
            return 0;
        }

        return warehouse.getAisles().size();
    }

    private static Integer countRackRows(Warehouse warehouse) {
        if (warehouse.getAisles() == null) {
            return 0;
        }

        return warehouse.getAisles()
                .stream()
                .map(Aisle::getRackRows)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }

    private static Integer countRackBays(Warehouse warehouse) {
        if (warehouse.getAisles() == null) {
            return 0;
        }

        return warehouse.getAisles()
                .stream()
                .map(Aisle::getRackRows)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(RackRow::getRackBays)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }

    private static Integer countRackLevels(Warehouse warehouse) {
        if (warehouse.getAisles() == null) {
            return 0;
        }

        return warehouse.getAisles()
                .stream()
                .map(Aisle::getRackRows)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(RackRow::getRackBays)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(RackBay::getRackLevels)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }

    private static Integer countStoragePlaces(Warehouse warehouse) {
        if (warehouse.getAisles() == null) {
            return 0;
        }

        return warehouse.getAisles()
                .stream()
                .map(Aisle::getRackRows)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(RackRow::getRackBays)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(RackBay::getRackLevels)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(RackLevel::getStoragePlaces)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }
}