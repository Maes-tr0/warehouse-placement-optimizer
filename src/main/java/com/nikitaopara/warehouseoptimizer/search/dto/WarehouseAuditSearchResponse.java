package com.nikitaopara.warehouseoptimizer.search.dto;

import java.util.List;

public record WarehouseAuditSearchResponse(
        List<WarehouseAuditEventResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public WarehouseAuditSearchResponse {
        items = List.copyOf(items);
    }
}
