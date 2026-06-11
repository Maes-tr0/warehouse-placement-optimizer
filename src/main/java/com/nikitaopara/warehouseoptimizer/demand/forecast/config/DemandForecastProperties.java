package com.nikitaopara.warehouseoptimizer.demand.forecast.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.demand-forecast")
@Getter
@Setter
public class DemandForecastProperties {

    private boolean enabled = true;
    private int forecastHorizonDays = 14;
    private int validationDays = 60;
    private int maximumLookbackDays = 1095;
    private int minimumArticleHistoryDays = 90;
    private int minimumTrainingSamples = 100;
    private int minimumValidationSamples = 20;
    private int maximumTreeDepth = 8;
    private double minimumImprovementPercent = 2.0;
    private int minimumRetrainingIntervalDays = 30;
    private int maximumRetrainingIntervalDays = 90;
    private int newObservationsThreshold = 200;
    private int staleTrainingHours = 24;
    private String trainingZone = "Europe/Amsterdam";
}
