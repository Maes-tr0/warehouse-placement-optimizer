package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.cache.config.CacheNames;
import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.demand.forecast.service.DemandForecastScoringService;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemandItem;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import com.nikitaopara.warehouseoptimizer.eventing.model.OptimizationAssessmentEventPayload;
import com.nikitaopara.warehouseoptimizer.eventing.model.WarehouseEventTopics;
import com.nikitaopara.warehouseoptimizer.eventing.service.DomainEventPublisher;
import com.nikitaopara.warehouseoptimizer.observability.WarehouseBusinessMetrics;
import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.dto.WarehouseOptimizationAssessmentResponse;
import com.nikitaopara.warehouseoptimizer.optimization.model.*;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationAssessmentRepository;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.service.WarehouseRouteCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseOptimizationAssessmentService {

    private final WarehouseRepository warehouseRepository;
    private final OrderDemandItemRepository orderDemandItemRepository;
    private final ContainerRepository containerRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final WarehouseOptimizationAssessmentRepository assessmentRepository;
    private final DemandForecastScoringService demandForecastScoringService;
    private final WarehouseEfficiencyCalculator efficiencyCalculator;
    private final WarehouseRouteCalculator routeCalculator;
    private final DomainEventPublisher eventPublisher;
    private final WarehouseBusinessMetrics businessMetrics;
    private final OptimizationProperties properties;

    @Transactional
    @CacheEvict(cacheNames = CacheNames.LATEST_ASSESSMENTS, key = "#warehouseId")
    public WarehouseOptimizationAssessmentResponse analyzeWarehouse(
            Long warehouseId,
            OptimizationAssessmentTrigger trigger
    ) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found: " + warehouseId
                ));

        LocalDateTime analyzedAt = LocalDateTime.now();
        LocalDateTime lookbackStart = analyzedAt.minusDays(properties.getLookbackDays());

        List<OrderDemandItem> demandItems = orderDemandItemRepository
                .findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
                        warehouseId,
                        lookbackStart,
                        analyzedAt
                );
        List<Container> storedContainers = containerRepository.findByWarehouseIdAndStatus(
                warehouseId,
                ContainerStatus.STORED
        );
        List<StoragePlace> storagePlaces = storagePlaceRepository
                .findByWarehouseIdOrderByDistanceFromEntryMmAsc(warehouseId);
        Map<Long, Integer> routeDistances = routeCalculator.calculateDistances(storagePlaces);

        List<DemandObservation> observations = demandItems.stream()
                .map(this::toDemandObservation)
                .toList();
        List<InventoryPosition> inventory = storedContainers.stream()
                .filter(container -> container.getCurrentStoragePlace() != null)
                .filter(container -> container.getArticle() != null)
                .filter(container -> container.getQuantity() != null && container.getQuantity() > 0)
                .filter(container -> routeDistances.containsKey(
                        container.getCurrentStoragePlace().getId()
                ))
                .map(container -> toInventoryPosition(container, routeDistances))
                .toList();

        WarehouseOptimizationAssessment assessment = calculateAssessment(
                warehouse,
                trigger,
                analyzedAt,
                lookbackStart,
                observations,
                inventory,
                storagePlaces,
                routeDistances
        );

        WarehouseOptimizationAssessment saved = assessmentRepository.save(assessment);
        eventPublisher.publish(
                WarehouseEventTopics.OPTIMIZATION,
                "warehouse.optimization.assessed",
                "WarehouseOptimizationAssessment",
                saved.getId().toString(),
                saved.getWarehouse().getCode(),
                OptimizationAssessmentEventPayload.from(saved)
        );
        businessMetrics.optimizationAssessed(saved);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.LATEST_ASSESSMENTS, key = "#warehouseId")
    public WarehouseOptimizationAssessmentResponse getLatestAssessment(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found: " + warehouseId);
        }

        WarehouseOptimizationAssessment assessment = assessmentRepository
                .findFirstByWarehouseIdOrderByAnalyzedAtDesc(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Optimization assessment not found for warehouse: " + warehouseId
                ));

        return toResponse(assessment);
    }

    private WarehouseOptimizationAssessment calculateAssessment(
            Warehouse warehouse,
            OptimizationAssessmentTrigger trigger,
            LocalDateTime analyzedAt,
            LocalDateTime lookbackStart,
            List<DemandObservation> observations,
            List<InventoryPosition> inventory,
            List<StoragePlace> storagePlaces,
            Map<Long, Integer> routeDistances
    ) {
        if (observations.size() < properties.getMinimumDemandObservations()
                || inventory.isEmpty()
                || storagePlaces.isEmpty()) {
            return buildInsufficientDataAssessment(
                    warehouse,
                    trigger,
                    analyzedAt,
                    lookbackStart,
                    observations.size(),
                    inventory.size()
            );
        }

        Map<Long, ArticleDemandScore> demandByArticle = demandForecastScoringService.calculate(
                warehouse.getId(),
                observations,
                analyzedAt.toLocalDate()
        );

        int nearestDistance = routeDistances.values().stream()
                .filter(distance -> distance >= 0)
                .min(Comparator.naturalOrder())
                .orElse(0);
        int farthestDistance = routeDistances.values().stream()
                .filter(distance -> distance >= 0)
                .max(Comparator.naturalOrder())
                .orElse(nearestDistance);

        WarehouseEfficiencyResult efficiency = efficiencyCalculator.calculate(
                inventory,
                demandByArticle,
                nearestDistance,
                farthestDistance
        );

        if (efficiency.scorePercent() == null) {
            return buildInsufficientDataAssessment(
                    warehouse,
                    trigger,
                    analyzedAt,
                    lookbackStart,
                    observations.size(),
                    inventory.size()
            );
        }

        OptimizationAssessmentStatus status = efficiency.scorePercent()
                .compareTo(properties.getThresholdPercent()) < 0
                ? OptimizationAssessmentStatus.OPTIMIZATION_RECOMMENDED
                : OptimizationAssessmentStatus.HEALTHY;

        return WarehouseOptimizationAssessment.builder()
                .warehouse(warehouse)
                .status(status)
                .trigger(trigger)
                .scorePercent(efficiency.scorePercent())
                .thresholdPercent(properties.getThresholdPercent())
                .weightedAverageDistanceMm(efficiency.weightedAverageDistanceMm())
                .lookbackStart(lookbackStart)
                .analyzedAt(analyzedAt)
                .demandObservationCount(observations.size())
                .analyzedContainerCount(inventory.size())
                .demandMatchedContainerCount(efficiency.demandMatchedContainers())
                .build();
    }

    private WarehouseOptimizationAssessment buildInsufficientDataAssessment(
            Warehouse warehouse,
            OptimizationAssessmentTrigger trigger,
            LocalDateTime analyzedAt,
            LocalDateTime lookbackStart,
            int demandObservationCount,
            int analyzedContainerCount
    ) {
        return WarehouseOptimizationAssessment.builder()
                .warehouse(warehouse)
                .status(OptimizationAssessmentStatus.INSUFFICIENT_DATA)
                .trigger(trigger)
                .thresholdPercent(properties.getThresholdPercent())
                .lookbackStart(lookbackStart)
                .analyzedAt(analyzedAt)
                .demandObservationCount(demandObservationCount)
                .analyzedContainerCount(analyzedContainerCount)
                .demandMatchedContainerCount(0)
                .build();
    }

    private DemandObservation toDemandObservation(OrderDemandItem item) {
        return new DemandObservation(
                item.getArticle().getId(),
                item.getOrderDemand().getId(),
                item.getOrderDemand().getOrderDateTime(),
                item.getQuantity()
        );
    }

    private InventoryPosition toInventoryPosition(
            Container container,
            Map<Long, Integer> routeDistances
    ) {
        return new InventoryPosition(
                container.getId(),
                container.getArticle().getId(),
                container.getQuantity(),
                routeDistances.get(container.getCurrentStoragePlace().getId())
        );
    }

    private WarehouseOptimizationAssessmentResponse toResponse(
            WarehouseOptimizationAssessment assessment
    ) {
        return new WarehouseOptimizationAssessmentResponse(
                assessment.getId(),
                assessment.getWarehouse().getId(),
                assessment.getStatus(),
                assessment.getTrigger(),
                assessment.getStatus() == OptimizationAssessmentStatus.OPTIMIZATION_RECOMMENDED,
                assessment.getScorePercent(),
                assessment.getThresholdPercent(),
                assessment.getWeightedAverageDistanceMm(),
                assessment.getLookbackStart(),
                assessment.getAnalyzedAt(),
                assessment.getDemandObservationCount(),
                assessment.getAnalyzedContainerCount(),
                assessment.getDemandMatchedContainerCount()
        );
    }
}
