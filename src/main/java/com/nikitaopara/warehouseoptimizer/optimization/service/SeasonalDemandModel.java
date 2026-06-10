package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;
import com.nikitaopara.warehouseoptimizer.optimization.model.DemandObservation;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SeasonalDemandModel {

    private static final double MINIMUM_SEASONAL_WEIGHT = 0.25;

    public Map<Long, ArticleDemandScore> calculate(
            List<DemandObservation> observations,
            LocalDate analysisDate,
            int recencyHalfLifeDays,
            int seasonalWindowDays
    ) {
        if (recencyHalfLifeDays <= 0 || seasonalWindowDays <= 0) {
            throw new IllegalArgumentException("Demand model windows must be positive");
        }

        Map<Long, MutableDemandScore> scores = new HashMap<>();

        for (DemandObservation observation : observations) {
            if (observation.articleId() == null
                    || observation.orderId() == null
                    || observation.orderedAt() == null
                    || observation.quantity() <= 0
                    || observation.orderedAt().toLocalDate().isAfter(analysisDate)) {
                continue;
            }

            LocalDate orderDate = observation.orderedAt().toLocalDate();
            long ageDays = ChronoUnit.DAYS.between(orderDate, analysisDate);
            double recencyWeight = Math.pow(0.5, (double) ageDays / recencyHalfLifeDays);
            double seasonalWeight = calculateSeasonalWeight(
                    orderDate,
                    analysisDate,
                    seasonalWindowDays
            );

            double weightedQuantity = observation.quantity() * recencyWeight * seasonalWeight;

            scores.computeIfAbsent(observation.articleId(), ignored -> new MutableDemandScore())
                    .add(observation.orderId(), observation.quantity(), weightedQuantity);
        }

        Map<Long, ArticleDemandScore> result = new HashMap<>();
        scores.forEach((articleId, score) -> result.put(
                articleId,
                new ArticleDemandScore(
                        articleId,
                        score.weightedDemand,
                        score.totalQuantity,
                        score.orderIds.size()
                )
        ));

        return Map.copyOf(result);
    }

    private double calculateSeasonalWeight(
            LocalDate orderDate,
            LocalDate analysisDate,
            int seasonalWindowDays
    ) {
        int orderDay = orderDate.getDayOfYear();
        int analysisDay = analysisDate.getDayOfYear();
        int yearLength = analysisDate.lengthOfYear();
        int directDistance = Math.abs(orderDay - analysisDay);
        int circularDistance = Math.min(directDistance, yearLength - directDistance);
        double normalizedDistance = (double) circularDistance / seasonalWindowDays;
        double gaussianKernel = Math.exp(-0.5 * normalizedDistance * normalizedDistance);

        return MINIMUM_SEASONAL_WEIGHT
                + (1.0 - MINIMUM_SEASONAL_WEIGHT) * gaussianKernel;
    }

    private static final class MutableDemandScore {
        private double weightedDemand;
        private long totalQuantity;
        private final Set<Long> orderIds = new HashSet<>();

        private void add(Long orderId, int quantity, double weightedQuantity) {
            weightedDemand += weightedQuantity;
            totalQuantity += quantity;
            orderIds.add(orderId);
        }
    }
}
