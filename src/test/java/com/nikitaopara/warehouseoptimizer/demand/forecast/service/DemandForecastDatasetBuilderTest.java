package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastDataset;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastRow;
import com.nikitaopara.warehouseoptimizer.optimization.model.DemandObservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DemandForecastDatasetBuilderTest {

    private DemandForecastProperties properties;
    private DemandForecastDatasetBuilder builder;

    @BeforeEach
    void setUp() {
        properties = new DemandForecastProperties();
        properties.setForecastHorizonDays(14);
        properties.setValidationDays(60);
        properties.setMaximumLookbackDays(400);
        properties.setMinimumArticleHistoryDays(90);
        builder = new DemandForecastDatasetBuilder(properties);
    }

    @Test
    void buildsColdStartBaselineOnForecastHorizonScale() {
        LocalDate featureDate = LocalDate.of(2026, 6, 10);
        List<DemandObservation> observations = List.of(
                observation(1L, 1L, featureDate, 5)
        );

        double result = builder.buildBaselineForecast(1L, observations, featureDate);

        assertThat(result).isEqualTo(2.5);
    }

    @Test
    void buildsChronologicalDatasetWithoutTargetLeakage() {
        LocalDate cutoff = LocalDate.of(2026, 6, 1);
        List<DemandObservation> observations = dailyDemand(
                10L,
                LocalDate.of(2025, 6, 1),
                cutoff,
                3
        );

        DemandForecastDataset dataset = builder.build(observations, cutoff);

        assertThat(dataset.trainingRows()).isNotEmpty();
        assertThat(dataset.validationRows()).isNotEmpty();
        assertThat(dataset.trainingRows())
                .allMatch(row -> row.featureDate().plusDays(properties.getForecastHorizonDays())
                        .isBefore(dataset.validationStart()));
        assertThat(dataset.validationRows())
                .allMatch(row -> !row.featureDate().isBefore(dataset.validationStart()));
        assertThat(dataset.validationRows())
                .allMatch(row -> !row.featureDate().plusDays(properties.getForecastHorizonDays())
                        .isAfter(cutoff));
    }

    @Test
    void calculatesRollingFeaturesAndFutureTarget() {
        LocalDate featureDate = LocalDate.of(2026, 5, 1);
        LocalDate firstDate = featureDate.minusDays(120);
        List<DemandObservation> observations = dailyDemand(
                20L,
                firstDate,
                featureDate.plusDays(14),
                2
        );

        DemandForecastRow row = builder.buildPredictionRow(20L, observations, featureDate);

        assertThat(feature(row, "quantity_lag_1")).isEqualTo(2.0);
        assertThat(feature(row, "quantity_sum_7")).isEqualTo(14.0);
        assertThat(feature(row, "quantity_sum_28")).isEqualTo(56.0);
        assertThat(feature(row, "active_days_28")).isEqualTo(28.0);
        assertThat(row.baselineQuantity()).isEqualTo(28.0);
        assertThat(row.targetQuantity()).isZero();
    }

    @Test
    void representsDaysWithoutOrdersAsZeroDemand() {
        LocalDate featureDate = LocalDate.of(2026, 5, 1);
        LocalDate firstDate = featureDate.minusDays(120);
        List<DemandObservation> observations = new ArrayList<>();
        observations.add(observation(30L, 1L, firstDate, 1));
        observations.add(observation(30L, 2L, featureDate.minusDays(6), 5));
        observations.add(observation(30L, 3L, featureDate, 7));

        DemandForecastRow row = builder.buildPredictionRow(30L, observations, featureDate);

        assertThat(feature(row, "quantity_lag_1")).isEqualTo(7.0);
        assertThat(feature(row, "quantity_lag_7")).isEqualTo(5.0);
        assertThat(feature(row, "quantity_sum_7")).isEqualTo(12.0);
        assertThat(feature(row, "active_days_28")).isEqualTo(2.0);
        assertThat(feature(row, "days_since_demand")).isZero();
    }

    private List<DemandObservation> dailyDemand(
            Long articleId,
            LocalDate from,
            LocalDate to,
            int quantity
    ) {
        List<DemandObservation> observations = new ArrayList<>();
        long orderId = 1L;

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            observations.add(observation(articleId, orderId++, date, quantity));
        }

        return observations;
    }

    private DemandObservation observation(
            Long articleId,
            Long orderId,
            LocalDate date,
            int quantity
    ) {
        return new DemandObservation(articleId, orderId, LocalDateTime.from(date.atStartOfDay()), quantity);
    }

    private double feature(DemandForecastRow row, String name) {
        for (int i = 0; i < row.featureNames().length; i++) {
            if (row.featureNames()[i].equals(name)) {
                return row.featureValues()[i];
            }
        }

        throw new AssertionError("Feature not found: " + name);
    }
}
