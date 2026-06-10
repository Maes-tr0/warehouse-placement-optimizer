package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;
import com.nikitaopara.warehouseoptimizer.optimization.model.InventoryPosition;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseEfficiencyResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarehouseEfficiencyCalculator {

    public WarehouseEfficiencyResult calculate(
            List<InventoryPosition> inventory,
            Map<Long, ArticleDemandScore> demandByArticle,
            int nearestDistanceMm,
            int farthestDistanceMm
    ) {
        if (nearestDistanceMm < 0 || farthestDistanceMm < nearestDistanceMm) {
            throw new IllegalArgumentException("Warehouse distance range is invalid");
        }

        Map<Long, Long> storedQuantityByArticle = calculateStoredQuantity(inventory);
        double totalDemandWeight = 0.0;
        double weightedDistance = 0.0;
        int matchedContainers = 0;

        for (InventoryPosition position : inventory) {
            ArticleDemandScore demandScore = demandByArticle.get(position.articleId());
            long storedQuantity = storedQuantityByArticle.getOrDefault(position.articleId(), 0L);

            if (demandScore == null || demandScore.weightedDemand() <= 0.0 || storedQuantity <= 0) {
                continue;
            }

            double containerShare = (double) position.quantity() / storedQuantity;
            double containerDemandWeight = demandScore.weightedDemand() * containerShare;

            totalDemandWeight += containerDemandWeight;
            weightedDistance += containerDemandWeight * position.distanceFromEntryMm();
            matchedContainers++;
        }

        if (totalDemandWeight <= 0.0) {
            return new WarehouseEfficiencyResult(null, null, 0);
        }

        double averageDistance = weightedDistance / totalDemandWeight;
        double score = calculateNormalizedScore(
                averageDistance,
                nearestDistanceMm,
                farthestDistanceMm
        );

        return new WarehouseEfficiencyResult(
                BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(averageDistance).setScale(2, RoundingMode.HALF_UP),
                matchedContainers
        );
    }

    private Map<Long, Long> calculateStoredQuantity(List<InventoryPosition> inventory) {
        Map<Long, Long> quantities = new HashMap<>();

        for (InventoryPosition position : inventory) {
            if (position.articleId() == null || position.quantity() <= 0) {
                continue;
            }

            quantities.merge(position.articleId(), (long) position.quantity(), Long::sum);
        }

        return quantities;
    }

    private double calculateNormalizedScore(
            double averageDistance,
            int nearestDistanceMm,
            int farthestDistanceMm
    ) {
        if (farthestDistanceMm == nearestDistanceMm) {
            return 100.0;
        }

        double normalizedDistance =
                (averageDistance - nearestDistanceMm) / (farthestDistanceMm - nearestDistanceMm);

        return Math.max(0.0, Math.min(100.0, (1.0 - normalizedDistance) * 100.0));
    }
}
