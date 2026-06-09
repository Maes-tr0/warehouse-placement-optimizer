package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementScoreResult;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PlacementScoringService {

    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(1_000_000);

    private final PlacementTimeEstimationService timeEstimationService;

    public PlacementScoreResult calculateMergeScore(
            Container sourceContainer,
            Container targetContainer
    ) {
        StoragePlace storagePlace = targetContainer.getCurrentStoragePlace();

        if (storagePlace == null) {
            throw new IllegalArgumentException("Target container must have current storage place");
        }

        Integer estimatedTimeSeconds =
                timeEstimationService.estimatePlacementTimeSeconds(storagePlace);

        BigDecimal score = calculateScoreFromTime(estimatedTimeSeconds);

        return new PlacementScoreResult(
                storagePlace.getDistanceFromEntryMm(),
                estimatedTimeSeconds,
                score,
                "Merge with existing container of the same article"
        );
    }

    public PlacementScoreResult calculatePlaceScore(
            Container sourceContainer,
            StoragePlace storagePlace
    ) {
        if (storagePlace == null) {
            throw new IllegalArgumentException("Storage place is required");
        }

        Integer estimatedTimeSeconds =
                timeEstimationService.estimatePlacementTimeSeconds(storagePlace);

        BigDecimal score = calculateScoreFromTime(estimatedTimeSeconds);

        return new PlacementScoreResult(
                storagePlace.getDistanceFromEntryMm(),
                estimatedTimeSeconds,
                score,
                "Place container into available storage place"
        );
    }

    private BigDecimal calculateScoreFromTime(Integer estimatedTimeSeconds) {
        if (estimatedTimeSeconds == null || estimatedTimeSeconds < 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }

        BigDecimal score = MAX_SCORE.subtract(BigDecimal.valueOf(estimatedTimeSeconds));

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }

        return score.setScale(3, RoundingMode.HALF_UP);
    }
}