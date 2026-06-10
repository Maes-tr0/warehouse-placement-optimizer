package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class DemandForecastRetrainingPolicy {

    private final DemandForecastProperties properties;

    public boolean shouldRetrain(
            DemandForecastModel activeModel,
            DemandForecastModel latestAttempt,
            long newObservationCount,
            LocalDate today
    ) {
        if (!properties.isEnabled()) {
            return false;
        }

        if (activeModel == null) {
            if (latestAttempt == null) {
                return true;
            }

            if (latestAttempt.getTrainedAt() == null) {
                return isStaleTraining(latestAttempt, today.atStartOfDay());
            }

            return ageInDays(latestAttempt, today)
                    >= properties.getMinimumRetrainingIntervalDays();
        }

        long ageDays = ageInDays(activeModel, today);

        if (ageDays >= properties.getMaximumRetrainingIntervalDays()) {
            return true;
        }

        if (ageDays < properties.getMinimumRetrainingIntervalDays()) {
            return false;
        }

        return newObservationCount >= properties.getNewObservationsThreshold();
    }

    private long ageInDays(DemandForecastModel model, LocalDate today) {
        return Math.max(
                0,
                ChronoUnit.DAYS.between(model.getTrainedAt().toLocalDate(), today)
        );
    }

    private boolean isStaleTraining(
            DemandForecastModel model,
            LocalDateTime now
    ) {
        if (model.getCreatedAt() == null) {
            return false;
        }

        return Duration.between(model.getCreatedAt(), now).toHours()
                >= properties.getStaleTrainingHours();
    }
}
