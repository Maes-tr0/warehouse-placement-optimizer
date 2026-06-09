package com.nikitaopara.warehouseoptimizer.demand.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ImportDemandOrderRequest(
        String orderNumber,
        LocalDateTime orderDateTime,
        List<ImportDemandOrderItemRequest> items
) {
}