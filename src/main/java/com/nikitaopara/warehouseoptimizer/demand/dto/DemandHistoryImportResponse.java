package com.nikitaopara.warehouseoptimizer.demand.dto;

public record DemandHistoryImportResponse(
        Long warehouseId,
        Integer totalOrders,
        Integer importedOrders,
        Integer skippedDuplicateOrders,
        Integer totalItems,
        Integer importedItems
) {
}