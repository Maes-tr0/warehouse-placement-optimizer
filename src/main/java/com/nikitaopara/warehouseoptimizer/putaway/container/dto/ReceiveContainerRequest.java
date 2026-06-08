package com.nikitaopara.warehouseoptimizer.putaway.container.dto;

import java.math.BigDecimal;

public record ReceiveContainerRequest(
        Long warehouseId,
        String containerNumber,
        String articleNumber,
        Integer quantity,
        BigDecimal weightKg,
        Integer heightMm
) {
}