package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendation;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendationType;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementScoreResult;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PlacementRecommendationFactory {

    private final Clock clock;
    private final Duration recommendationTtl;

    @Autowired
    public PlacementRecommendationFactory(
            @Value("${app.placement.recommendation-ttl-minutes:15}") long recommendationTtlMinutes
    ) {
        this(Clock.systemDefaultZone(), recommendationTtlMinutes);
    }

    PlacementRecommendationFactory(Clock clock, long recommendationTtlMinutes) {
        if (recommendationTtlMinutes <= 0) {
            throw new IllegalArgumentException("Recommendation TTL must be greater than zero");
        }

        this.clock = clock;
        this.recommendationTtl = Duration.ofMinutes(recommendationTtlMinutes);
    }

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
                .expiresAt(calculateExpiresAt())
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
                .expiresAt(calculateExpiresAt())
                .build();
    }

    private LocalDateTime calculateExpiresAt() {
        return LocalDateTime.now(clock).plus(recommendationTtl);
    }

    private String generateCode() {
        return "REC-" + UUID.randomUUID();
    }
}
