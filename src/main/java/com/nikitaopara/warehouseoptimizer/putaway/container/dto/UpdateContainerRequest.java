package com.nikitaopara.warehouseoptimizer.putaway.container.dto;

import java.math.BigDecimal;

public record UpdateContainerRequest(
        Integer quantity,
        BigDecimal weightKg,
        Integer heightMm
) {
}