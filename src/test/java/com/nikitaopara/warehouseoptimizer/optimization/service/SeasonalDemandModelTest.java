package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.optimization.model.DemandObservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeasonalDemandModelTest {

    private final SeasonalDemandModel model = new SeasonalDemandModel();

    @Test
    void prioritizesRecentDemandFromTheCurrentSeason() {
        LocalDate analysisDate = LocalDate.of(2026, 6, 10);
        List<DemandObservation> observations = List.of(
                new DemandObservation(1L, 10L, LocalDateTime.of(2026, 6, 9, 10, 0), 10),
                new DemandObservation(2L, 20L, LocalDateTime.of(2025, 12, 10, 10, 0), 10)
        );

        var scores = model.calculate(observations, analysisDate, 365, 45);

        assertThat(scores.get(1L).weightedDemand())
                .isGreaterThan(scores.get(2L).weightedDemand());
    }

    @Test
    void countsDistinctOrdersAndIgnoresFutureObservations() {
        LocalDate analysisDate = LocalDate.of(2026, 6, 10);
        List<DemandObservation> observations = List.of(
                new DemandObservation(1L, 10L, LocalDateTime.of(2026, 6, 1, 10, 0), 3),
                new DemandObservation(1L, 10L, LocalDateTime.of(2026, 6, 1, 10, 0), 2),
                new DemandObservation(1L, 11L, LocalDateTime.of(2026, 6, 2, 10, 0), 4),
                new DemandObservation(1L, 12L, LocalDateTime.of(2026, 6, 11, 10, 0), 100)
        );

        var score = model.calculate(observations, analysisDate, 365, 45).get(1L);

        assertThat(score.totalQuantity()).isEqualTo(9);
        assertThat(score.orderCount()).isEqualTo(2);
    }
}
