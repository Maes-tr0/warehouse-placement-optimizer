package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastDataset;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastRow;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.TrainedDemandForecast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TribuoDemandForecastTrainerTest {

    private TribuoDemandForecastTrainer trainer;

    @BeforeEach
    void setUp() {
        DemandForecastProperties properties = new DemandForecastProperties();
        properties.setMaximumTreeDepth(6);
        trainer = new TribuoDemandForecastTrainer(properties);
    }

    @Test
    void trainsValidatesAndRestoresRegressionModel() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        List<DemandForecastRow> training = rows(start, 160);
        List<DemandForecastRow> validation = rows(start.plusDays(160), 40);
        DemandForecastDataset dataset = new DemandForecastDataset(
                training,
                validation,
                1,
                200,
                start,
                start.plusDays(199),
                start.plusDays(160)
        );

        TrainedDemandForecast trained = trainer.train(dataset);
        double restoredPrediction = trainer.predict(
                trainer.deserialize(trained.artifact()),
                validation.getFirst()
        );

        assertThat(trained.artifact()).isNotEmpty();
        assertThat(trained.metrics().modelMae())
                .isLessThan(trained.metrics().baselineMae());
        assertThat(trained.metrics().improvementPercent()).isPositive();
        assertThat(restoredPrediction).isGreaterThanOrEqualTo(0.0);
    }

    private List<DemandForecastRow> rows(LocalDate start, int count) {
        List<DemandForecastRow> rows = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            double signal = index % 20;
            rows.add(new DemandForecastRow(
                    1L,
                    start.plusDays(index),
                    signal * 3.0 + 5.0,
                    100.0,
                    new String[]{"signal", "weekly"},
                    new double[]{signal, index % 7}
            ));
        }

        return rows;
    }
}
