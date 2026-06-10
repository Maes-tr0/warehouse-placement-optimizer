package com.nikitaopara.warehouseoptimizer.optimization.dto;

public record CompleteRelocationStepRequest(
        String sourceContainerNumber,
        String targetStoragePlaceCode,
        String targetContainerNumber
) {
}
