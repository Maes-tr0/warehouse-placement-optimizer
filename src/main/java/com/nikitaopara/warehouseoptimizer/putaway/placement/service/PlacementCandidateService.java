package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDataService;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDimensionCalculationService;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlacementCandidateService {

    private final ContainerDataService containerDataService;
    private final ContainerDimensionCalculationService dimensionCalculationService;
    private final StoragePlaceRepository storagePlaceRepository;
    private final PlacementRecommendationDataService recommendationDataService;

    public Optional<Container> findBestMergeRecommendationCandidate(Container sourceContainer) {
        Long warehouseId = sourceContainer.getWarehouse().getId();
        Set<Long> suggestedTargetContainerIds =
                recommendationDataService.getSuggestedMergeTargetContainerIds(warehouseId);
        Set<Long> previouslyRecommendedTargetContainerIds =
                recommendationDataService.getPreviouslyRecommendedTargetContainerIds(sourceContainer);

        Set<Long> preferredBlockedIds = new HashSet<>(suggestedTargetContainerIds);
        preferredBlockedIds.addAll(previouslyRecommendedTargetContainerIds);

        return findBestMergeCandidate(sourceContainer, preferredBlockedIds)
                .or(() -> findBestMergeCandidate(sourceContainer, suggestedTargetContainerIds));
    }

    private Optional<Container> findBestMergeCandidate(
            Container sourceContainer,
            Set<Long> blockedTargetContainerIds
    ) {
        Long warehouseId = sourceContainer.getWarehouse().getId();
        Long articleId = sourceContainer.getArticle().getId();

        return containerDataService.getStoredContainersByWarehouseAndArticle(warehouseId, articleId)
                .stream()
                .filter(targetContainer -> !isSameContainer(sourceContainer, targetContainer))
                .filter(targetContainer -> !blockedTargetContainerIds.contains(targetContainer.getId()))
                .filter(targetContainer -> !targetContainer.isReservedForOptimization())
                .filter(this::hasCurrentStoragePlace)
                .filter(targetContainer -> targetContainer.canAcceptQuantity(sourceContainer.getQuantity()))
                .filter(targetContainer -> canTargetContainerFitAfterMerge(sourceContainer, targetContainer))
                .min(Comparator
                        .comparingInt(this::getContainerDistanceFromEntry)
                        .thenComparing(Container::getContainerNumber)
                );
    }

    public Optional<StoragePlace> findBestPlaceRecommendationCandidate(Container sourceContainer) {
        Long warehouseId = sourceContainer.getWarehouse().getId();
        Set<Long> suggestedStoragePlaceIds =
                recommendationDataService.getSuggestedStoragePlaceIds(warehouseId);
        Set<Long> previouslyRecommendedStoragePlaceIds =
                recommendationDataService.getPreviouslyRecommendedStoragePlaceIds(sourceContainer);

        Set<Long> preferredBlockedIds = new HashSet<>(suggestedStoragePlaceIds);
        preferredBlockedIds.addAll(previouslyRecommendedStoragePlaceIds);

        return findBestStoragePlaceCandidate(sourceContainer, preferredBlockedIds)
                .or(() -> findBestStoragePlaceCandidate(sourceContainer, suggestedStoragePlaceIds));
    }

    private Optional<StoragePlace> findBestStoragePlaceCandidate(
            Container sourceContainer,
            Set<Long> blockedStoragePlaceIds
    ) {
        Long warehouseId = sourceContainer.getWarehouse().getId();

        return storagePlaceRepository
                .findByWarehouseIdAndStatusOrderByDistanceFromEntryMmAsc(
                        warehouseId,
                        StoragePlaceStatus.AVAILABLE
                )
                .stream()
                .filter(storagePlace -> !blockedStoragePlaceIds.contains(storagePlace.getId()))
                .filter(storagePlace -> !storagePlace.isReservedForOptimization())
                .filter(storagePlace -> canStoragePlaceFitContainer(storagePlace, sourceContainer))
                .min(Comparator
                        .comparingInt(this::getStoragePlaceDistanceFromEntry)
                        .thenComparing(StoragePlace::getCode)
                );
    }

    private boolean canTargetContainerFitAfterMerge(
            Container sourceContainer,
            Container targetContainer
    ) {
        StoragePlace storagePlace = targetContainer.getCurrentStoragePlace();

        if (storagePlace == null) {
            return false;
        }

        Integer mergedQuantity = targetContainer.getQuantity() + sourceContainer.getQuantity();

        BigDecimal mergedWeightKg = dimensionCalculationService.calculateWeightKg(
                targetContainer.getArticle(),
                mergedQuantity
        );

        Integer mergedHeightMm = dimensionCalculationService.calculateHeightMm(
                targetContainer.getArticle(),
                mergedQuantity
        );

        return canStoragePlaceFitWeightAndHeight(
                storagePlace,
                mergedWeightKg,
                mergedHeightMm
        );
    }

    private boolean canStoragePlaceFitContainer(
            StoragePlace storagePlace,
            Container container
    ) {
        if (storagePlace == null || container == null) {
            return false;
        }

        return canStoragePlaceFitWeightAndHeight(
                storagePlace,
                container.getWeightKg(),
                container.getHeightMm()
        );
    }

    private boolean canStoragePlaceFitWeightAndHeight(
            StoragePlace storagePlace,
            BigDecimal weightKg,
            Integer heightMm
    ) {
        if (storagePlace == null || weightKg == null || heightMm == null) {
            return false;
        }

        boolean weightFits =
                weightKg.compareTo(BigDecimal.valueOf(storagePlace.getMaxWeightKg())) <= 0;

        boolean heightFits =
                heightMm <= storagePlace.getMaxHeightMm();

        return weightFits && heightFits;
    }

    private boolean hasCurrentStoragePlace(Container container) {
        return container.getCurrentStoragePlace() != null;
    }

    private boolean isSameContainer(Container sourceContainer, Container targetContainer) {
        return Objects.equals(sourceContainer.getId(), targetContainer.getId());
    }

    private int getContainerDistanceFromEntry(Container container) {
        StoragePlace storagePlace = container.getCurrentStoragePlace();

        if (storagePlace == null || storagePlace.getDistanceFromEntryMm() == null) {
            return Integer.MAX_VALUE;
        }

        return storagePlace.getDistanceFromEntryMm();
    }

    private int getStoragePlaceDistanceFromEntry(StoragePlace storagePlace) {
        if (storagePlace == null || storagePlace.getDistanceFromEntryMm() == null) {
            return Integer.MAX_VALUE;
        }

        return storagePlace.getDistanceFromEntryMm();
    }
}
