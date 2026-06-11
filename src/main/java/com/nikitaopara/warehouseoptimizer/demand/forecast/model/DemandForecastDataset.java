package com.nikitaopara.warehouseoptimizer.demand.forecast.model;

import java.time.LocalDate;
import java.util.List;

public record DemandForecastDataset(
        List<DemandForecastRow> trainingRows,
        List<DemandForecastRow> validationRows,
        int articleCount,
        int observationCount,
        LocalDate dataStart,
        LocalDate dataCutoff,
        LocalDate validationStart
) {

    public boolean hasEnoughSamples(int minimumTrainingSamples, int minimumValidationSamples) {
        return trainingRows.size() >= minimumTrainingSamples
                && validationRows.size() >= minimumValidationSamples;
    }
}
