package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DemandForecastRetrainingPolicyTest {

    private DemandForecastProperties properties;
    private DemandForecastRetrainingPolicy policy;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        properties = new DemandForecastProperties();
        properties.setMinimumRetrainingIntervalDays(30);
        properties.setMaximumRetrainingIntervalDays(90);
        properties.setNewObservationsThreshold(200);
        policy = new DemandForecastRetrainingPolicy(properties);
        today = LocalDate.of(2026, 6, 10);
    }

    @Test
    void trainsImmediatelyWhenNoAttemptExists() {
        assertThat(policy.shouldRetrain(null, null, 0, today)).isTrue();
    }

    @Test
    void waitsMinimumIntervalAfterRejectedOrFailedAttempt() {
        DemandForecastModel latest = modelTrainedDaysAgo(20);

        assertThat(policy.shouldRetrain(null, latest, 0, today)).isFalse();

        latest.setTrainedAt(today.minusDays(30).atStartOfDay());
        assertThat(policy.shouldRetrain(null, latest, 0, today)).isTrue();
    }

    @Test
    void retriesTrainingAttemptThatWasLeftStale() {
        DemandForecastModel latest = DemandForecastModel.builder()
                .createdAt(today.minusDays(2).atStartOfDay())
                .build();

        assertThat(policy.shouldRetrain(null, latest, 0, today)).isTrue();
    }

    @Test
    void retrainsAfterEnoughNewObservationsAndMinimumInterval() {
        DemandForecastModel active = modelTrainedDaysAgo(30);

        assertThat(policy.shouldRetrain(active, active, 199, today)).isFalse();
        assertThat(policy.shouldRetrain(active, active, 200, today)).isTrue();
    }

    @Test
    void forcesRetrainingAtMaximumInterval() {
        DemandForecastModel active = modelTrainedDaysAgo(90);

        assertThat(policy.shouldRetrain(active, active, 0, today)).isTrue();
    }

    private DemandForecastModel modelTrainedDaysAgo(int days) {
        return DemandForecastModel.builder()
                .trainedAt(LocalDateTime.from(today.minusDays(days).atStartOfDay()))
                .build();
    }
}
