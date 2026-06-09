package com.nikitaopara.warehouseoptimizer.putaway.container.dto;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.UnitType;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContainerResponse(
        Long id,
        String containerNumber,

        Long warehouseId,
        String warehouseCode,

        Long articleId,
        String articleNumber,
        String articleName,
        UnitType unitType,

        Integer quantity,
        BigDecimal weightKg,
        Integer heightMm,

        Long currentStoragePlaceId,
        String currentStoragePlaceCode,

        ContainerStatus status,

        Long mergedIntoContainerId,
        String mergedIntoContainerNumber,

        LocalDateTime receivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ContainerResponse from(Container container) {
        StoragePlace storagePlace = container.getCurrentStoragePlace();
        Container mergedIntoContainer = container.getMergedIntoContainer();

        return new ContainerResponse(
                container.getId(),
                container.getContainerNumber(),

                container.getWarehouse() != null ? container.getWarehouse().getId() : null,
                container.getWarehouse() != null ? container.getWarehouse().getCode() : null,

                container.getArticle() != null ? container.getArticle().getId() : null,
                container.getArticle() != null ? container.getArticle().getArticleNumber() : null,
                container.getArticle() != null ? container.getArticle().getName() : null,
                container.getArticle() != null ? container.getArticle().getUnitType() : null,

                container.getQuantity(),
                container.getWeightKg(),
                container.getHeightMm(),

                storagePlace != null ? storagePlace.getId() : null,
                storagePlace != null ? storagePlace.getCode() : null,

                container.getStatus(),

                mergedIntoContainer != null ? mergedIntoContainer.getId() : null,
                mergedIntoContainer != null ? mergedIntoContainer.getContainerNumber() : null,

                container.getReceivedAt(),
                container.getCreatedAt(),
                container.getUpdatedAt()
        );
    }
}