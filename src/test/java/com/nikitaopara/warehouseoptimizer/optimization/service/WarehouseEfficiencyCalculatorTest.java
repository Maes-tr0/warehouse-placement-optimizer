package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;
import com.nikitaopara.warehouseoptimizer.optimization.model.InventoryPosition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseEfficiencyCalculatorTest {

    private final WarehouseEfficiencyCalculator calculator = new WarehouseEfficiencyCalculator();

    @Test
    void givesHighScoreWhenPopularInventoryIsCloseToEntry() {
        List<InventoryPosition> inventory = List.of(
                new InventoryPosition(1L, 10L, 100, 1_000),
                new InventoryPosition(2L, 20L, 100, 9_000)
        );
        Map<Long, ArticleDemandScore> demand = Map.of(
                10L, new ArticleDemandScore(10L, 90.0, 100, 10),
                20L, new ArticleDemandScore(20L, 10.0, 100, 2)
        );

        var result = calculator.calculate(inventory, demand, 1_000, 9_000);

        assertThat(result.scorePercent()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(result.weightedAverageDistanceMm())
                .isEqualByComparingTo(new BigDecimal("1800.00"));
    }

    @Test
    void distributesArticleDemandAcrossItsContainersByQuantity() {
        List<InventoryPosition> inventory = List.of(
                new InventoryPosition(1L, 10L, 25, 1_000),
                new InventoryPosition(2L, 10L, 75, 9_000)
        );
        Map<Long, ArticleDemandScore> demand = Map.of(
                10L, new ArticleDemandScore(10L, 100.0, 100, 10)
        );

        var result = calculator.calculate(inventory, demand, 1_000, 9_000);

        assertThat(result.scorePercent()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.demandMatchedContainers()).isEqualTo(2);
    }

    @Test
    void reportsMissingScoreWhenDemandDoesNotMatchStoredInventory() {
        List<InventoryPosition> inventory = List.of(
                new InventoryPosition(1L, 10L, 100, 1_000)
        );

        var result = calculator.calculate(inventory, Map.of(), 1_000, 9_000);

        assertThat(result.scorePercent()).isNull();
        assertThat(result.weightedAverageDistanceMm()).isNull();
        assertThat(result.demandMatchedContainers()).isZero();
    }
}
