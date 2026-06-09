package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendation;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendationStatus;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendationType;
import com.nikitaopara.warehouseoptimizer.putaway.placement.repository.PlacementRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlacementRecommendationDataService {

    private final PlacementRecommendationRepository placementRecommendationRepository;

    public PlacementRecommendation save(PlacementRecommendation recommendation) {
        return placementRecommendationRepository.save(recommendation);
    }

    public PlacementRecommendation getByCodeOrThrow(String code) {
        return placementRecommendationRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Placement recommendation not found by code: " + code
                ));
    }

    public Optional<PlacementRecommendation> findSuggestedRecommendationBySourceContainer(
            Container sourceContainer
    ) {
        if (sourceContainer == null || sourceContainer.getId() == null) {
            return Optional.empty();
        }

        return placementRecommendationRepository
                .findFirstBySourceContainerIdAndStatusOrderByCreatedAtDesc(
                        sourceContainer.getId(),
                        PlacementRecommendationStatus.SUGGESTED
                );
    }

    public List<PlacementRecommendation> getRecommendationsBySourceContainer(Container sourceContainer) {
        if (sourceContainer == null || sourceContainer.getId() == null) {
            return List.of();
        }

        return placementRecommendationRepository
                .findBySourceContainerIdOrderByCreatedAtDesc(sourceContainer.getId());
    }

    public Set<Long> getSuggestedStoragePlaceIds(Long warehouseId) {
        return placementRecommendationRepository
                .findRecommendedStoragePlaceIdsByWarehouseIdAndStatus(
                        warehouseId,
                        PlacementRecommendationStatus.SUGGESTED
                );
    }

    public Set<Long> getSuggestedMergeTargetContainerIds(Long warehouseId) {
        return placementRecommendationRepository
                .findTargetContainerIdsByWarehouseIdAndStatusAndRecommendationType(
                        warehouseId,
                        PlacementRecommendationStatus.SUGGESTED,
                        PlacementRecommendationType.MERGE
                );
    }

    public Set<Long> getPreviouslyRecommendedStoragePlaceIds(Container sourceContainer) {
        if (sourceContainer == null || sourceContainer.getId() == null) {
            return Set.of();
        }

        return placementRecommendationRepository
                .findPreviouslyRecommendedStoragePlaceIdsBySourceContainerId(sourceContainer.getId());
    }

    public Set<Long> getPreviouslyRecommendedTargetContainerIds(Container sourceContainer) {
        if (sourceContainer == null || sourceContainer.getId() == null) {
            return Set.of();
        }

        return placementRecommendationRepository
                .findPreviouslyRecommendedTargetContainerIdsBySourceContainerId(sourceContainer.getId());
    }

    public PlacementRecommendation reject(PlacementRecommendation recommendation) {
        recommendation.reject();

        return placementRecommendationRepository.save(recommendation);
    }

    public PlacementRecommendation accept(PlacementRecommendation recommendation) {
        recommendation.accept();

        return placementRecommendationRepository.save(recommendation);
    }
}