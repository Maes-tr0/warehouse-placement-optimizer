package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovementType;
import com.nikitaopara.warehouseoptimizer.movement.service.ContainerMovementService;
import com.nikitaopara.warehouseoptimizer.optimization.dto.CompleteRelocationStepRequest;
import com.nikitaopara.warehouseoptimizer.optimization.model.*;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationPlanRepository;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDimensionCalculationService;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseRelocationExecutionServiceTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private WarehouseOptimizationPlanRepository planRepository;

    @Mock
    private ContainerMovementService movementService;

    private WarehouseRelocationExecutionService executionService;

    @BeforeEach
    void setUp() {
        executionService = new WarehouseRelocationExecutionService(
                authenticatedUserService,
                planRepository,
                new ContainerDimensionCalculationService(),
                movementService
        );
    }

    @Test
    void completesScannedMoveAndFinishesSingleStepPlan() {
        User operator = User.builder().id(1L).role(Role.OPERATOR).build();
        WarehouseOptimizationPlan plan = singleMovePlan();
        WarehouseRelocationStep step = plan.getSteps().getFirst();
        StoragePlace from = step.getFromStoragePlace();
        StoragePlace to = step.getToStoragePlace();
        Container source = step.getSourceContainer();

        when(authenticatedUserService.getCurrentUser()).thenReturn(operator);
        when(planRepository.findForUpdateByCode(plan.getCode())).thenReturn(Optional.of(plan));
        when(planRepository.save(plan)).thenReturn(plan);

        var response = executionService.completeCurrentStep(
                plan.getCode(),
                new CompleteRelocationStepRequest("C-1", "B-01", null)
        );

        assertThat(response.planStatus()).isEqualTo(OptimizationPlanStatus.COMPLETED);
        assertThat(response.nextStep()).isNull();
        assertThat(step.getStatus()).isEqualTo(RelocationStepStatus.COMPLETED);
        assertThat(source.getCurrentStoragePlace()).isSameAs(to);
        assertThat(from.getStatus()).isEqualTo(StoragePlaceStatus.AVAILABLE);
        assertThat(to.getStatus()).isEqualTo(StoragePlaceStatus.OCCUPIED);
        verify(movementService).recordOptimizationMovement(
                eq(plan),
                eq(step),
                eq(source),
                isNull(),
                eq(from),
                eq(to),
                eq(50),
                eq(ContainerMovementType.RELOCATION),
                eq(operator)
        );
    }

    @Test
    void rejectsContainerThatDoesNotMatchReadyStep() {
        User operator = User.builder().id(1L).role(Role.OPERATOR).build();
        WarehouseOptimizationPlan plan = singleMovePlan();

        when(authenticatedUserService.getCurrentUser()).thenReturn(operator);
        when(planRepository.findForUpdateByCode(plan.getCode())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> executionService.completeCurrentStep(
                plan.getCode(),
                new CompleteRelocationStepRequest("WRONG", "B-01", null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source container");

        verifyNoInteractions(movementService);
        verify(planRepository, never()).save(any());
    }

    private WarehouseOptimizationPlan singleMovePlan() {
        Warehouse warehouse = Warehouse.builder().id(10L).code("WH-1").build();
        Article article = Article.builder()
                .id(11L)
                .articleNumber("A-1")
                .unitWidthMm(200)
                .unitLengthMm(300)
                .unitHeightMm(100)
                .unitWeightKg(BigDecimal.ONE)
                .maxQuantityPerPallet(100)
                .build();
        StoragePlace from = StoragePlace.builder()
                .id(12L)
                .code("A-01")
                .status(StoragePlaceStatus.OCCUPIED)
                .maxWeightKg(1_000)
                .maxHeightMm(2_000)
                .build();
        StoragePlace to = StoragePlace.builder()
                .id(13L)
                .code("B-01")
                .status(StoragePlaceStatus.AVAILABLE)
                .maxWeightKg(1_000)
                .maxHeightMm(2_000)
                .build();
        Container container = Container.builder()
                .id(14L)
                .containerNumber("C-1")
                .warehouse(warehouse)
                .article(article)
                .quantity(50)
                .weightKg(BigDecimal.valueOf(50))
                .heightMm(500)
                .currentStoragePlace(from)
                .status(ContainerStatus.STORED)
                .build();
        WarehouseOptimizationPlan plan = WarehouseOptimizationPlan.builder()
                .id(15L)
                .code("OPT-1")
                .warehouse(warehouse)
                .status(OptimizationPlanStatus.APPROVED)
                .build();
        plan.addStep(WarehouseRelocationStep.builder()
                .id(16L)
                .sequenceNumber(1)
                .type(RelocationStepType.MOVE)
                .status(RelocationStepStatus.READY)
                .sourceContainer(container)
                .fromStoragePlace(from)
                .toStoragePlace(to)
                .estimatedTimeSavingSeconds(10L)
                .reason("Move closer")
                .build());

        return plan;
    }
}
