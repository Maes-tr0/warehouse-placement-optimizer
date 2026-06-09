package com.nikitaopara.warehouseoptimizer.putaway.container.dto;

import java.util.List;

public record ReceiveContainersBatchRequest(
        List<ReceiveContainerRequest> containers
) {
}