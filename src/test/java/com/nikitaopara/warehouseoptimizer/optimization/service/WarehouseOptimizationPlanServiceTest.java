package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.demand.forecast.service.DemandForecastScoringService;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.*;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationAssessmentRepository;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationPlanRepository;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseOptimizationPlanServiceTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private WarehouseOptimizationAssessmentRepository assessmentRepository;
    @Mock
    private WarehouseOptimizationPlanRepository planRepository;
    @Mock
    private OrderDemandItemRepository orderDemandItemRepository;
    @Mock
    private ContainerRepository containerRepository;
    @Mock
    private StoragePlaceRepository storagePlaceRepository;
    @Mock
    private DemandForecastScoringService demandForecastScoringService;
    @Mock
    private WarehouseRelocationPlanner relocationPlanner;

    private WarehouseOptimizationPlanService service;

    @BeforeEach
    void setUp() {
        service = new WarehouseOptimizationPlanService(
                authenticatedUserService,
                assessmentRepository,
                planRepository,
                orderDemandItemRepository,
                containerRepository,
                storagePlaceRepository,
                demandForecastScoringService,
                relocationPlanner,
                new OptimizationProperties()
        );
    }

    @Test
    void cancelsInProgressPlanSoRemainingWorkDoesNotStayBlocked() {
        User actor = User.builder().role(Role.ADMIN).build();
        Warehouse warehouse = Warehouse.builder().id(11L).build();
        WarehouseOptimizationAssessment assessment = WarehouseOptimizationAssessment.builder()
                .id(22L)
                .warehouse(warehouse)
                .build();
        WarehouseOptimizationPlan plan = WarehouseOptimizationPlan.builder()
                .code("OPT-1")
                .warehouse(warehouse)
                .assessment(assessment)
                .status(OptimizationPlanStatus.IN_PROGRESS)
                .initialScorePercent(BigDecimal.valueOf(50))
                .targetScorePercent(BigDecimal.valueOf(85))
                .projectedScorePercent(BigDecimal.valueOf(80))
                .estimatedTimeSavingSeconds(120L)
                .build();

        when(authenticatedUserService.getCurrentUser()).thenReturn(actor);
        when(planRepository.findForUpdateByCode("OPT-1")).thenReturn(Optional.of(plan));
        when(planRepository.save(plan)).thenReturn(plan);

        var response = service.cancelPlan("OPT-1");

        assertThat(response.status()).isEqualTo(OptimizationPlanStatus.CANCELLED);
        verify(planRepository).save(plan);
    }

    @Test
    void reservesPlanContainersAndPlacesWhenApproved() {
        User actor = User.builder().role(Role.ADMIN).build();
        Warehouse warehouse = Warehouse.builder().id(11L).build();
        Article article = Article.builder().id(12L).articleNumber("100").build();
        StoragePlace from = StoragePlace.builder().id(13L).code("A-1").build();
        StoragePlace to = StoragePlace.builder().id(14L).code("B-1").build();
        Container source = Container.builder()
                .id(15L)
                .containerNumber("C-1")
                .article(article)
                .build();
        WarehouseOptimizationAssessment assessment = WarehouseOptimizationAssessment.builder()
                .id(22L)
                .warehouse(warehouse)
                .build();
        WarehouseOptimizationPlan plan = WarehouseOptimizationPlan.builder()
                .code("OPT-2")
                .warehouse(warehouse)
                .assessment(assessment)
                .status(OptimizationPlanStatus.DRAFT)
                .initialScorePercent(BigDecimal.valueOf(50))
                .targetScorePercent(BigDecimal.valueOf(85))
                .projectedScorePercent(BigDecimal.valueOf(80))
                .estimatedTimeSavingSeconds(120L)
                .build();
        plan.addStep(WarehouseRelocationStep.builder()
                .sequenceNumber(1)
                .type(RelocationStepType.MOVE)
                .status(RelocationStepStatus.PENDING)
                .sourceContainer(source)
                .fromStoragePlace(from)
                .toStoragePlace(to)
                .estimatedTimeSavingSeconds(120L)
                .reason("Move closer")
                .build());

        when(authenticatedUserService.getCurrentUser()).thenReturn(actor);
        when(planRepository.findForUpdateByCode("OPT-2")).thenReturn(Optional.of(plan));
        when(planRepository.save(plan)).thenReturn(plan);

        var response = service.approvePlan("OPT-2");

        assertThat(response.status()).isEqualTo(OptimizationPlanStatus.APPROVED);
        assertThat(source.getOptimizationReservationCode()).isEqualTo("OPT-2");
        assertThat(from.getOptimizationReservationCode()).isEqualTo("OPT-2");
        assertThat(to.getOptimizationReservationCode()).isEqualTo("OPT-2");
        verify(containerRepository).saveAll(java.util.Set.of(source));
        verify(storagePlaceRepository).saveAll(java.util.Set.of(from, to));
    }
}
