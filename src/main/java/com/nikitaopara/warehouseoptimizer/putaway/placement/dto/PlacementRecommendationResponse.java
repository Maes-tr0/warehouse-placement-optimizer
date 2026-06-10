package com.nikitaopara.warehouseoptimizer.putaway.placement.dto;

import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendation;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendationStatus;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlacementRecommendationResponse(
        Long id,
        String code,

        PlacementRecommendationType recommendationType,
        PlacementRecommendationStatus status,

        String containerNumber,

        String targetContainerNumber,
        String targetStoragePlaceCode,

        String recommendedStoragePlaceCode,

        Integer distanceFromEntryMm,
        Integer estimatedTimeSeconds,
        BigDecimal score,

        String reason,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime expiresAt
) {

    public static PlacementRecommendationResponse from(PlacementRecommendation recommendation) {
        return new PlacementRecommendationResponse(
                recommendation.getId(),
                recommendation.getCode(),

                recommendation.getRecommendationType(),
                recommendation.getStatus(),

                recommendation.getSourceContainer() != null
                        ? recommendation.getSourceContainer().getContainerNumber()
                        : null,

                recommendation.getTargetContainer() != null
                        ? recommendation.getTargetContainer().getContainerNumber()
                        : null,

                recommendation.getRecommendationType() == PlacementRecommendationType.MERGE
                        && recommendation.getRecommendedStoragePlace() != null
                        ? recommendation.getRecommendedStoragePlace().getCode()
                        : null,

                recommendation.getRecommendationType() == PlacementRecommendationType.PLACE
                        && recommendation.getRecommendedStoragePlace() != null
                        ? recommendation.getRecommendedStoragePlace().getCode()
                        : null,

                recommendation.getDistanceFromEntryMm(),
                recommendation.getEstimatedTimeSeconds(),
                recommendation.getScore(),

                recommendation.getReason(),

                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt(),
                recommendation.getExpiresAt()
        );
    }
}
