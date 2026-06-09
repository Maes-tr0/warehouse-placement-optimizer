package com.nikitaopara.warehouseoptimizer.putaway.container.dto;

import java.util.List;

public record ReceiveContainersBatchResponse(
        Integer totalContainers,
        Integer receivedContainers,
        List<ContainerResponse> containers
) {
}