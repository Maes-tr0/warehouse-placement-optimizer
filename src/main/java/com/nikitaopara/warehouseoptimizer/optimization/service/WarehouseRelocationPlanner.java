package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.*;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDimensionCalculationService;
import com.nikitaopara.warehouseoptimizer.putaway.placement.service.PlacementTimeEstimationService;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseRelocationPlanner {

    private final ContainerDimensionCalculationService dimensionCalculationService;
    private final PlacementTimeEstimationService timeEstimationService;
    private final WarehouseEfficiencyCalculator efficiencyCalculator;
    private final OptimizationProperties properties;

    public RelocationPlanDraft createPlan(
            List<Container> storedContainers,
            List<StoragePlace> storagePlaces,
            Map<Long, ArticleDemandScore> demandByArticle
    ) {
        Map<Long, VirtualContainer> virtualContainers = createVirtualContainers(
                storedContainers,
                demandByArticle
        );
        Map<Long, StoragePlace> placesById = storagePlaces.stream()
                .collect(Collectors.toMap(StoragePlace::getId, place -> place));
        Set<Long> availablePlaceIds = storagePlaces.stream()
                .filter(place -> place.getStatus() == StoragePlaceStatus.AVAILABLE)
                .map(StoragePlace::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<PlannedRelocationStep> steps = new ArrayList<>();
        long estimatedSavingSeconds = 0;

        estimatedSavingSeconds += planConsolidations(
                virtualContainers,
                availablePlaceIds,
                steps
        );

        while (steps.size() < properties.getMaximumPlanSteps()) {
            BigDecimal score = calculateProjectedScore(
                    virtualContainers.values(),
                    storagePlaces,
                    demandByArticle
            );

            if (score.compareTo(properties.getTargetPercent()) >= 0) {
                break;
            }

            Optional<MoveCandidate> directMove = findBestDirectMove(
                    virtualContainers.values(),
                    availablePlaceIds,
                    placesById
            );

            if (directMove.isPresent()) {
                MoveCandidate move = directMove.get();
                applyDirectMove(move, availablePlaceIds, steps);
                estimatedSavingSeconds += move.savingSeconds();
                continue;
            }

            Optional<SwapCandidate> swap = findBestSwap(
                    virtualContainers.values(),
                    availablePlaceIds,
                    placesById
            );

            if (swap.isEmpty() || steps.size() + 3 > properties.getMaximumPlanSteps()) {
                break;
            }

            SwapCandidate candidate = swap.get();
            applySwap(candidate, steps);
            estimatedSavingSeconds += candidate.savingSeconds();
        }

        BigDecimal projectedScore = calculateProjectedScore(
                virtualContainers.values(),
                storagePlaces,
                demandByArticle
        );

        return new RelocationPlanDraft(
                List.copyOf(steps),
                projectedScore,
                estimatedSavingSeconds
        );
    }

    private Map<Long, VirtualContainer> createVirtualContainers(
            List<Container> containers,
            Map<Long, ArticleDemandScore> demandByArticle
    ) {
        Map<Long, Integer> totalQuantityByArticle = containers.stream()
                .filter(container -> container.getArticle() != null)
                .collect(Collectors.groupingBy(
                        container -> container.getArticle().getId(),
                        Collectors.summingInt(Container::getQuantity)
                ));
        Map<Long, VirtualContainer> result = new LinkedHashMap<>();

        for (Container container : containers) {
            if (container.getCurrentStoragePlace() == null || container.getArticle() == null) {
                continue;
            }

            ArticleDemandScore demand = demandByArticle.get(container.getArticle().getId());
            int totalArticleQuantity = totalQuantityByArticle.getOrDefault(
                    container.getArticle().getId(),
                    0
            );
            double demandWeight = demand == null || totalArticleQuantity == 0
                    ? 0.0
                    : demand.weightedDemand() * container.getQuantity() / totalArticleQuantity;

            result.put(container.getId(), new VirtualContainer(
                    container,
                    container.getQuantity(),
                    container.getCurrentStoragePlace(),
                    demandWeight
            ));
        }

        return result;
    }

    private long planConsolidations(
            Map<Long, VirtualContainer> containers,
            Set<Long> availablePlaceIds,
            List<PlannedRelocationStep> steps
    ) {
        long totalSaving = 0;
        Map<Long, List<VirtualContainer>> byArticle = containers.values().stream()
                .collect(Collectors.groupingBy(container -> container.entity().getArticle().getId()));

        for (List<VirtualContainer> articleContainers : byArticle.values()) {
            articleContainers.sort(Comparator
                    .comparingInt(VirtualContainer::quantity)
                    .thenComparing(container -> container.entity().getContainerNumber()));

            for (VirtualContainer source : new ArrayList<>(articleContainers)) {
                if (!containers.containsKey(source.entity().getId())
                        || steps.size() >= properties.getMaximumPlanSteps()) {
                    continue;
                }

                Optional<VirtualContainer> targetCandidate = articleContainers.stream()
                        .filter(target -> containers.containsKey(target.entity().getId()))
                        .filter(target -> !Objects.equals(
                                source.entity().getId(),
                                target.entity().getId()
                        ))
                        .filter(target -> canMerge(source, target))
                        .max(Comparator.comparingInt(VirtualContainer::quantity));

                if (targetCandidate.isEmpty()) {
                    continue;
                }

                VirtualContainer target = targetCandidate.get();
                long saving = calculateContainerPickingCost(source);

                steps.add(new PlannedRelocationStep(
                        RelocationStepType.MERGE,
                        source.entity().getId(),
                        target.entity().getId(),
                        source.place().getId(),
                        target.place().getId(),
                        saving,
                        "Consolidate partial pallets of article "
                                + source.entity().getArticle().getArticleNumber()
                ));

                target.add(source.quantity(), source.demandWeight());
                availablePlaceIds.add(source.place().getId());
                containers.remove(source.entity().getId());
                totalSaving += saving;
            }
        }

        return totalSaving;
    }

    private boolean canMerge(VirtualContainer source, VirtualContainer target) {
        int mergedQuantity = source.quantity() + target.quantity();

        if (!target.entity().getArticle().canFitQuantity(mergedQuantity)) {
            return false;
        }

        BigDecimal mergedWeight = dimensionCalculationService.calculateWeightKg(
                target.entity().getArticle(),
                mergedQuantity
        );
        int mergedHeight = dimensionCalculationService.calculateHeightMm(
                target.entity().getArticle(),
                mergedQuantity
        );

        return mergedWeight.compareTo(BigDecimal.valueOf(target.place().getMaxWeightKg())) <= 0
                && mergedHeight <= target.place().getMaxHeightMm();
    }

    private Optional<MoveCandidate> findBestDirectMove(
            Collection<VirtualContainer> containers,
            Set<Long> availablePlaceIds,
            Map<Long, StoragePlace> placesById
    ) {
        MoveCandidate best = null;

        for (VirtualContainer container : containers) {
            if (container.demandWeight() <= 0.0) {
                continue;
            }

            for (Long placeId : availablePlaceIds) {
                StoragePlace target = placesById.get(placeId);

                if (!canFit(container, target)) {
                    continue;
                }

                long saving = calculateMoveSaving(container, container.place(), target);

                if (saving < properties.getMinimumTimeSavingSeconds()) {
                    continue;
                }

                MoveCandidate candidate = new MoveCandidate(container, container.place(), target, saving);

                if (best == null || candidate.savingSeconds() > best.savingSeconds()) {
                    best = candidate;
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private Optional<SwapCandidate> findBestSwap(
            Collection<VirtualContainer> containers,
            Set<Long> availablePlaceIds,
            Map<Long, StoragePlace> placesById
    ) {
        List<StoragePlace> buffers = availablePlaceIds.stream()
                .map(placesById::get)
                .filter(Objects::nonNull)
                .toList();
        SwapCandidate best = null;

        for (VirtualContainer first : containers) {
            for (VirtualContainer second : containers) {
                if (first.entity().getId() >= second.entity().getId()) {
                    continue;
                }

                if (!canFit(first, second.place()) || !canFit(second, first.place())) {
                    continue;
                }

                Optional<StoragePlace> buffer = buffers.stream()
                        .filter(place -> canFit(second, place))
                        .findFirst();

                if (buffer.isEmpty()) {
                    continue;
                }

                long before = calculateContainerPickingCost(first)
                        + calculateContainerPickingCost(second);
                long after = calculatePickingCost(first, second.place())
                        + calculatePickingCost(second, first.place());
                long saving = before - after;

                if (saving < properties.getMinimumTimeSavingSeconds()) {
                    continue;
                }

                SwapCandidate candidate = new SwapCandidate(first, second, buffer.get(), saving);

                if (best == null || candidate.savingSeconds() > best.savingSeconds()) {
                    best = candidate;
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private void applyDirectMove(
            MoveCandidate move,
            Set<Long> availablePlaceIds,
            List<PlannedRelocationStep> steps
    ) {
        steps.add(new PlannedRelocationStep(
                RelocationStepType.MOVE,
                move.container().entity().getId(),
                null,
                move.from().getId(),
                move.to().getId(),
                move.savingSeconds(),
                "Move higher-demand article closer to the warehouse entry"
        ));

        availablePlaceIds.remove(move.to().getId());
        availablePlaceIds.add(move.from().getId());
        move.container().moveTo(move.to());
    }

    private void applySwap(SwapCandidate swap, List<PlannedRelocationStep> steps) {
        StoragePlace firstPlace = swap.first().place();
        StoragePlace secondPlace = swap.second().place();

        steps.add(new PlannedRelocationStep(
                RelocationStepType.TEMPORARY_MOVE,
                swap.second().entity().getId(),
                null,
                secondPlace.getId(),
                swap.buffer().getId(),
                0,
                "Temporarily free a higher-priority storage place"
        ));
        steps.add(new PlannedRelocationStep(
                RelocationStepType.MOVE,
                swap.first().entity().getId(),
                null,
                firstPlace.getId(),
                secondPlace.getId(),
                swap.savingSeconds(),
                "Swap a higher-demand pallet into the faster picking location"
        ));
        steps.add(new PlannedRelocationStep(
                RelocationStepType.MOVE,
                swap.second().entity().getId(),
                null,
                swap.buffer().getId(),
                firstPlace.getId(),
                0,
                "Complete the pallet swap and release the buffer location"
        ));

        swap.first().moveTo(secondPlace);
        swap.second().moveTo(firstPlace);
    }

    private boolean canFit(VirtualContainer container, StoragePlace place) {
        return place != null
                && container.entity().getWeightKg().compareTo(
                        BigDecimal.valueOf(place.getMaxWeightKg())
                ) <= 0
                && container.entity().getHeightMm() <= place.getMaxHeightMm();
    }

    private long calculateMoveSaving(
            VirtualContainer container,
            StoragePlace from,
            StoragePlace to
    ) {
        return calculatePickingCost(container, from) - calculatePickingCost(container, to);
    }

    private long calculateContainerPickingCost(VirtualContainer container) {
        return calculatePickingCost(container, container.place());
    }

    private long calculatePickingCost(VirtualContainer container, StoragePlace place) {
        return Math.round(
                container.demandWeight() * timeEstimationService.estimatePlacementTimeSeconds(place)
        );
    }

    private BigDecimal calculateProjectedScore(
            Collection<VirtualContainer> containers,
            List<StoragePlace> places,
            Map<Long, ArticleDemandScore> demandByArticle
    ) {
        if (places.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<InventoryPosition> inventory = containers.stream()
                .map(container -> new InventoryPosition(
                        container.entity().getId(),
                        container.entity().getArticle().getId(),
                        container.quantity(),
                        container.place().getDistanceFromEntryMm()
                ))
                .toList();
        int nearest = places.stream()
                .mapToInt(StoragePlace::getDistanceFromEntryMm)
                .min()
                .orElse(0);
        int farthest = places.stream()
                .mapToInt(StoragePlace::getDistanceFromEntryMm)
                .max()
                .orElse(nearest);
        WarehouseEfficiencyResult result = efficiencyCalculator.calculate(
                inventory,
                demandByArticle,
                nearest,
                farthest
        );

        return result.scorePercent() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : result.scorePercent();
    }

    private static final class VirtualContainer {
        private final Container entity;
        private int quantity;
        private StoragePlace place;
        private double demandWeight;

        private VirtualContainer(
                Container entity,
                int quantity,
                StoragePlace place,
                double demandWeight
        ) {
            this.entity = entity;
            this.quantity = quantity;
            this.place = place;
            this.demandWeight = demandWeight;
        }

        private Container entity() {
            return entity;
        }

        private int quantity() {
            return quantity;
        }

        private StoragePlace place() {
            return place;
        }

        private double demandWeight() {
            return demandWeight;
        }

        private void add(int additionalQuantity, double additionalDemandWeight) {
            quantity += additionalQuantity;
            demandWeight += additionalDemandWeight;
        }

        private void moveTo(StoragePlace target) {
            place = target;
        }
    }

    private record MoveCandidate(
            VirtualContainer container,
            StoragePlace from,
            StoragePlace to,
            long savingSeconds
    ) {
    }

    private record SwapCandidate(
            VirtualContainer first,
            VirtualContainer second,
            StoragePlace buffer,
            long savingSeconds
    ) {
    }
}
