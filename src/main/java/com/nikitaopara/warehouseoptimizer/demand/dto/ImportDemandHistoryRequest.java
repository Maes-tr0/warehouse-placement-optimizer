package com.nikitaopara.warehouseoptimizer.demand.dto;

import java.util.List;

public record ImportDemandHistoryRequest(
        Long warehouseId,
        List<ImportDemandOrderRequest> orders
) {
}