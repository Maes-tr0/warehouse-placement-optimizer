package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemand;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemandItem;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import com.nikitaopara.warehouseoptimizer.demand.forecast.service.DemandForecastScoringService;
import com.nikitaopara.warehouseoptimizer.eventing.service.DomainEventPublisher;
import com.nikitaopara.warehouseoptimizer.observability.WarehouseBusinessMetrics;
import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentStatus;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentTrigger;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationAssessmentRepository;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.service.DijkstraWarehouseRouter;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.service.WarehouseGraphBuilder;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.service.WarehouseRouteCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseOptimizationAssessmentServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private OrderDemandItemRepository orderDemandItemRepository;

    @Mock
    private ContainerRepository containerRepository;

    @Mock
    private StoragePlaceRepository storagePlaceRepository;

    @Mock
    private WarehouseOptimizationAssessmentRepository assessmentRepository;

    @Mock
    private DemandForecastScoringService demandForecastScoringService;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private WarehouseBusinessMetrics businessMetrics;

    private WarehouseOptimizationAssessmentService assessmentService;

    @BeforeEach
    void setUp() {
        OptimizationProperties properties = new OptimizationProperties();
        properties.setMinimumDemandObservations(1);

        assessmentService = new WarehouseOptimizationAssessmentService(
                warehouseRepository,
                orderDemandItemRepository,
                containerRepository,
                storagePlaceRepository,
                assessmentRepository,
                demandForecastScoringService,
                new WarehouseEfficiencyCalculator(),
                new WarehouseRouteCalculator(
                        new WarehouseGraphBuilder(),
                        new DijkstraWarehouseRouter()
                ),
                eventPublisher,
                businessMetrics,
                properties
        );
    }

    @Test
    void recommendsOptimizationWhenPopularInventoryIsAtTheFarthestPlace() {
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-1").build();
        Article article = Article.builder().id(2L).articleNumber("A-1").build();
        StoragePlace nearPlace = StoragePlace.builder()
                .id(3L)
                .code("NEAR")
                .distanceFromEntryMm(1_000)
                .build();
        StoragePlace farPlace = StoragePlace.builder()
                .id(4L)
                .code("FAR")
                .distanceFromEntryMm(9_000)
                .build();
        OrderDemand order = OrderDemand.builder()
                .id(5L)
                .warehouse(warehouse)
                .orderNumber("ORDER-1")
                .orderDateTime(LocalDateTime.now().minusDays(1))
                .build();
        OrderDemandItem demandItem = OrderDemandItem.builder()
                .id(6L)
                .warehouse(warehouse)
                .orderDemand(order)
                .article(article)
                .quantity(20)
                .build();
        Container container = Container.builder()
                .id(7L)
                .warehouse(warehouse)
                .article(article)
                .quantity(100)
                .currentStoragePlace(farPlace)
                .status(ContainerStatus.STORED)
                .build();

        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(orderDemandItemRepository.findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
                any(),
                any(),
                any()
        )).thenReturn(List.of(demandItem));
        when(containerRepository.findByWarehouseIdAndStatus(
                warehouse.getId(),
                ContainerStatus.STORED
        )).thenReturn(List.of(container));
        when(storagePlaceRepository.findByWarehouseIdOrderByDistanceFromEntryMmAsc(warehouse.getId()))
                .thenReturn(List.of(nearPlace, farPlace));
        when(assessmentRepository.save(any())).thenAnswer(invocation -> {
            var assessment = invocation.getArgument(
                    0,
                    com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationAssessment.class
            );
            assessment.setId(8L);
            return assessment;
        });
        when(demandForecastScoringService.calculate(any(), any(), any())).thenReturn(
                java.util.Map.of(
                        article.getId(),
                        new ArticleDemandScore(article.getId(), 20.0, 20, 1)
                )
        );

        var response = assessmentService.analyzeWarehouse(
                warehouse.getId(),
                OptimizationAssessmentTrigger.MANUAL
        );

        assertThat(response.status()).isEqualTo(OptimizationAssessmentStatus.OPTIMIZATION_RECOMMENDED);
        assertThat(response.optimizationRecommended()).isTrue();
        assertThat(response.scorePercent()).isZero();
    }
}
