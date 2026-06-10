package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;
import com.nikitaopara.warehouseoptimizer.optimization.model.RelocationStepType;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDimensionCalculationService;
import com.nikitaopara.warehouseoptimizer.putaway.placement.service.PlacementTimeEstimationService;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseRelocationPlannerTest {

    private WarehouseRelocationPlanner planner;

    @BeforeEach
    void setUp() {
        OptimizationProperties properties = new OptimizationProperties();
        properties.setTargetPercent(new BigDecimal("85.00"));
        properties.setMaximumPlanSteps(10);

        planner = new WarehouseRelocationPlanner(
                new ContainerDimensionCalculationService(),
                new PlacementTimeEstimationService(),
                new WarehouseEfficiencyCalculator(),
                properties
        );
    }

    @Test
    void movesPopularContainerIntoAvailableCloserPlace() {
        Article article = article(1L, "POPULAR");
        StoragePlace near = place(10L, "NEAR", 1_000, StoragePlaceStatus.AVAILABLE);
        StoragePlace far = place(11L, "FAR", 9_000, StoragePlaceStatus.OCCUPIED);
        Container container = container(20L, "C-1", article, 100, far);

        var draft = planner.createPlan(
                List.of(container),
                List.of(near, far),
                Map.of(article.getId(), new ArticleDemandScore(article.getId(), 100, 100, 20))
        );

        assertThat(draft.steps()).hasSize(1);
        assertThat(draft.steps().getFirst().type()).isEqualTo(RelocationStepType.MOVE);
        assertThat(draft.steps().getFirst().toStoragePlaceId()).isEqualTo(near.getId());
        assertThat(draft.projectedScorePercent()).isEqualByComparingTo("100.00");
    }

    @Test
    void decomposesOccupiedPlaceSwapIntoThreeExecutableSteps() {
        Article popularArticle = article(1L, "POPULAR");
        Article slowArticle = article(2L, "SLOW");
        StoragePlace near = place(10L, "NEAR", 1_000, StoragePlaceStatus.OCCUPIED);
        StoragePlace far = place(11L, "FAR", 9_000, StoragePlaceStatus.OCCUPIED);
        StoragePlace buffer = place(12L, "BUFFER", 10_000, StoragePlaceStatus.AVAILABLE);
        Container popular = container(20L, "C-POPULAR", popularArticle, 100, far);
        Container slow = container(21L, "C-SLOW", slowArticle, 100, near);

        var draft = planner.createPlan(
                List.of(popular, slow),
                List.of(near, far, buffer),
                Map.of(
                        popularArticle.getId(), new ArticleDemandScore(1L, 90, 100, 20),
                        slowArticle.getId(), new ArticleDemandScore(2L, 10, 100, 2)
                )
        );

        assertThat(draft.steps()).hasSize(3);
        assertThat(draft.steps()).extracting(step -> step.type()).containsExactly(
                RelocationStepType.TEMPORARY_MOVE,
                RelocationStepType.MOVE,
                RelocationStepType.MOVE
        );
        assertThat(draft.steps().getFirst().sourceContainerId()).isEqualTo(slow.getId());
        assertThat(draft.steps().getFirst().toStoragePlaceId()).isEqualTo(buffer.getId());
        assertThat(draft.steps().get(1).sourceContainerId()).isEqualTo(popular.getId());
        assertThat(draft.steps().get(1).toStoragePlaceId()).isEqualTo(near.getId());
    }

    @Test
    void consolidatesCompatiblePartialPalletsBeforeRelocation() {
        Article article = article(1L, "PARTIAL");
        StoragePlace firstPlace = place(10L, "A", 1_000, StoragePlaceStatus.OCCUPIED);
        StoragePlace secondPlace = place(11L, "B", 2_000, StoragePlaceStatus.OCCUPIED);
        Container first = container(20L, "C-1", article, 20, firstPlace);
        Container second = container(21L, "C-2", article, 30, secondPlace);

        var draft = planner.createPlan(
                List.of(first, second),
                List.of(firstPlace, secondPlace),
                Map.of(article.getId(), new ArticleDemandScore(article.getId(), 50, 50, 10))
        );

        assertThat(draft.steps()).isNotEmpty();
        assertThat(draft.steps().getFirst().type()).isEqualTo(RelocationStepType.MERGE);
        assertThat(draft.steps().getFirst().sourceContainerId()).isEqualTo(first.getId());
        assertThat(draft.steps().getFirst().targetContainerId()).isEqualTo(second.getId());
    }

    private Article article(Long id, String number) {
        return Article.builder()
                .id(id)
                .articleNumber(number)
                .unitWidthMm(200)
                .unitLengthMm(300)
                .unitHeightMm(100)
                .unitWeightKg(BigDecimal.ONE)
                .maxQuantityPerPallet(100)
                .build();
    }

    private StoragePlace place(
            Long id,
            String code,
            int distance,
            StoragePlaceStatus status
    ) {
        return StoragePlace.builder()
                .id(id)
                .code(code)
                .distanceFromEntryMm(distance)
                .maxWeightKg(1_000)
                .maxHeightMm(2_000)
                .status(status)
                .build();
    }

    private Container container(
            Long id,
            String number,
            Article article,
            int quantity,
            StoragePlace place
    ) {
        return Container.builder()
                .id(id)
                .containerNumber(number)
                .article(article)
                .quantity(quantity)
                .weightKg(BigDecimal.valueOf(quantity))
                .heightMm(500)
                .currentStoragePlace(place)
                .status(ContainerStatus.STORED)
                .build();
    }
}
