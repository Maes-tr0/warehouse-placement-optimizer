package com.nikitaopara.warehouseoptimizer.demand.forecast.dto;

import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModelStatus;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastTrainingTrigger;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DemandForecastModelResponse(
        String code,
        Long warehouseId,
        int versionNumber,
        DemandForecastModelStatus status,
        DemandForecastTrainingTrigger trainingTrigger,
        String algorithm,
        int featureSchemaVersion,
        int forecastHorizonDays,
        LocalDate trainingStart,
        LocalDate trainingEnd,
        LocalDate validationStart,
        LocalDate validationEnd,
        LocalDate dataCutoff,
        int observationCount,
        int articleCount,
        int trainingSampleCount,
        int validationSampleCount,
        Double modelMae,
        Double baselineMae,
        Double modelRmse,
        Double modelR2,
        Double improvementPercent,
        String message,
        LocalDateTime trainedAt
) {
}
