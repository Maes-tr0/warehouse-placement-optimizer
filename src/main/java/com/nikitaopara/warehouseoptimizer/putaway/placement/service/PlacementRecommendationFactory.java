package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendation;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendationType;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementScoreResult;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlacementRecommendationFactory {

    public PlacementRecommendation createMergeRecommendation(
            Container sourceContainer,
            Container targetContainer,
            PlacementScoreResult scoreResult
    ) {
        return PlacementRecommendation.builder()
                .code(generateCode())
                .warehouse(sourceContainer.getWarehouse())
                .sourceContainer(sourceContainer)
                .targetContainer(targetContainer)
                .recommendedStoragePlace(targetContainer.getCurrentStoragePlace())
                .recommendationType(PlacementRecommendationType.MERGE)
                .distanceFromEntryMm(scoreResult.distanceFromEntryMm())
                .estimatedTimeSeconds(scoreResult.estimatedTimeSeconds())
                .score(scoreResult.score())
                .reason(scoreResult.reason())
                .build();
    }

    public PlacementRecommendation createPlaceRecommendation(
            Container sourceContainer,
            StoragePlace storagePlace,
            PlacementScoreResult scoreResult
    ) {
        return PlacementRecommendation.builder()
                .code(generateCode())
                .warehouse(sourceContainer.getWarehouse())
                .sourceContainer(sourceContainer)
                .recommendedStoragePlace(storagePlace)
                .recommendationType(PlacementRecommendationType.PLACE)
                .distanceFromEntryMm(scoreResult.distanceFromEntryMm())
                .estimatedTimeSeconds(scoreResult.estimatedTimeSeconds())
                .score(scoreResult.score())
                .reason(scoreResult.reason())
                .build();
    }

    private String generateCode() {
        return "REC-" + UUID.randomUUID();
    }
}