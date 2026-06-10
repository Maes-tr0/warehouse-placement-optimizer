package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DailyArticleDemand;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastDataset;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastRow;
import com.nikitaopara.warehouseoptimizer.optimization.model.DemandObservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DemandForecastDatasetBuilder {

    public static final String[] FEATURE_NAMES = {
            "quantity_lag_1",
            "quantity_lag_7",
            "quantity_lag_14",
            "quantity_lag_28",
            "quantity_sum_7",
            "quantity_sum_28",
            "quantity_sum_90",
            "quantity_mean_7",
            "quantity_mean_28",
            "quantity_mean_90",
            "active_days_28",
            "order_count_28",
            "days_since_demand",
            "article_age_days",
            "short_long_trend",
            "day_of_week_sin",
            "day_of_week_cos",
            "day_of_year_sin",
            "day_of_year_cos"
    };

    private static final int LONGEST_FEATURE_WINDOW_DAYS = 90;

    private final DemandForecastProperties properties;

    public DemandForecastDataset build(
            List<DemandObservation> observations,
            LocalDate dataCutoff
    ) {
        validateConfiguration();

        LocalDate earliestAllowedDate = dataCutoff.minusDays(properties.getMaximumLookbackDays());
        List<DemandObservation> validObservations = observations.stream()
                .filter(this::isValid)
                .filter(observation -> !observation.orderedAt().toLocalDate().isAfter(dataCutoff))
                .filter(observation -> !observation.orderedAt().toLocalDate().isBefore(earliestAllowedDate))
                .sorted(Comparator.comparing(DemandObservation::orderedAt))
                .toList();

        Map<Long, ArticleSeries> seriesByArticle = aggregate(validObservations);
        LocalDate validationStart = dataCutoff.minusDays(properties.getValidationDays() - 1L);
        LocalDate latestFeatureDate = dataCutoff.minusDays(properties.getForecastHorizonDays());
        LocalDate latestTrainingFeatureDate = validationStart
                .minusDays(properties.getForecastHorizonDays() + 1L);
        List<DemandForecastRow> trainingRows = new ArrayList<>();
        List<DemandForecastRow> validationRows = new ArrayList<>();

        for (ArticleSeries series : seriesByArticle.values()) {
            LocalDate firstFeatureDate = series.firstDemandDate()
                    .plusDays(Math.max(
                            LONGEST_FEATURE_WINDOW_DAYS - 1L,
                            properties.getMinimumArticleHistoryDays() - 1L
                    ));

            for (LocalDate featureDate = firstFeatureDate;
                 !featureDate.isAfter(latestFeatureDate);
                 featureDate = featureDate.plusDays(1)) {
                DemandForecastRow row = createRow(series, featureDate);

                if (!featureDate.isAfter(latestTrainingFeatureDate)) {
                    trainingRows.add(row);
                } else if (!featureDate.isBefore(validationStart)) {
                    validationRows.add(row);
                }
            }
        }

        LocalDate dataStart = validObservations.isEmpty()
                ? dataCutoff
                : validObservations.getFirst().orderedAt().toLocalDate();

        return new DemandForecastDataset(
                List.copyOf(trainingRows),
                List.copyOf(validationRows),
                seriesByArticle.size(),
                validObservations.size(),
                dataStart,
                dataCutoff,
                validationStart
        );
    }

    public DemandForecastRow buildPredictionRow(
            Long articleId,
            List<DemandObservation> observations,
            LocalDate featureDate
    ) {
        ArticleSeries series = aggregate(observations.stream()
                .filter(this::isValid)
                .filter(observation -> articleId.equals(observation.articleId()))
                .filter(observation -> !observation.orderedAt().toLocalDate().isAfter(featureDate))
                .toList())
                .get(articleId);

        if (series == null) {
            throw new IllegalArgumentException("Article has no valid demand history: " + articleId);
        }

        long historyDays = ChronoUnit.DAYS.between(series.firstDemandDate(), featureDate) + 1;

        if (historyDays < properties.getMinimumArticleHistoryDays()) {
            throw new IllegalArgumentException("Article has insufficient demand history: " + articleId);
        }

        return createRow(series, featureDate);
    }

    private Map<Long, ArticleSeries> aggregate(List<DemandObservation> observations) {
        Map<Long, Map<LocalDate, MutableDailyDemand>> aggregated = new LinkedHashMap<>();

        for (DemandObservation observation : observations) {
            LocalDate date = observation.orderedAt().toLocalDate();
            MutableDailyDemand dailyDemand = aggregated
                    .computeIfAbsent(observation.articleId(), ignored -> new HashMap<>())
                    .computeIfAbsent(date, ignored -> new MutableDailyDemand());
            dailyDemand.quantity += observation.quantity();
            dailyDemand.orderIds.add(observation.orderId());
        }

        Map<Long, ArticleSeries> result = new LinkedHashMap<>();
        aggregated.forEach((articleId, byDate) -> {
            LocalDate firstDate = byDate.keySet().stream().min(LocalDate::compareTo).orElseThrow();
            Map<LocalDate, DailyArticleDemand> days = new HashMap<>();
            byDate.forEach((date, demand) -> days.put(
                    date,
                    new DailyArticleDemand(articleId, date, demand.quantity, demand.orderIds.size())
            ));
            result.put(articleId, new ArticleSeries(articleId, firstDate, days));
        });

        return result;
    }

    private DemandForecastRow createRow(ArticleSeries series, LocalDate featureDate) {
        double quantity7 = sumQuantity(series, featureDate, 7);
        double quantity28 = sumQuantity(series, featureDate, 28);
        double quantity90 = sumQuantity(series, featureDate, 90);
        double trend = quantity28 == 0.0
                ? 0.0
                : (quantity7 / 7.0) / (quantity28 / 28.0);
        double dayOfWeekAngle = 2.0 * Math.PI
                * (featureDate.getDayOfWeek().getValue() - 1) / 7.0;
        double dayOfYearAngle = 2.0 * Math.PI
                * (featureDate.getDayOfYear() - 1) / featureDate.lengthOfYear();
        double[] values = {
                quantityOn(series, featureDate),
                quantityOn(series, featureDate.minusDays(6)),
                quantityOn(series, featureDate.minusDays(13)),
                quantityOn(series, featureDate.minusDays(27)),
                quantity7,
                quantity28,
                quantity90,
                quantity7 / 7.0,
                quantity28 / 28.0,
                quantity90 / 90.0,
                activeDays(series, featureDate, 28),
                orderCount(series, featureDate, 28),
                daysSinceDemand(series, featureDate),
                ChronoUnit.DAYS.between(series.firstDemandDate(), featureDate) + 1,
                trend,
                Math.sin(dayOfWeekAngle),
                Math.cos(dayOfWeekAngle),
                Math.sin(dayOfYearAngle),
                Math.cos(dayOfYearAngle)
        };
        double target = sumQuantity(
                series,
                featureDate.plusDays(1),
                properties.getForecastHorizonDays(),
                true
        );
        double baseline = quantity28 / 28.0 * properties.getForecastHorizonDays();

        return new DemandForecastRow(
                series.articleId(),
                featureDate,
                target,
                baseline,
                FEATURE_NAMES.clone(),
                values
        );
    }

    private double sumQuantity(ArticleSeries series, LocalDate endDate, int days) {
        return sumQuantity(series, endDate, days, false);
    }

    private double sumQuantity(
            ArticleSeries series,
            LocalDate date,
            int days,
            boolean forward
    ) {
        double total = 0.0;

        for (int offset = 0; offset < days; offset++) {
            LocalDate currentDate = forward
                    ? date.plusDays(offset)
                    : date.minusDays(offset);
            total += quantityOn(series, currentDate);
        }

        return total;
    }

    private int activeDays(ArticleSeries series, LocalDate endDate, int days) {
        int count = 0;

        for (int offset = 0; offset < days; offset++) {
            if (quantityOn(series, endDate.minusDays(offset)) > 0) {
                count++;
            }
        }

        return count;
    }

    private int orderCount(ArticleSeries series, LocalDate endDate, int days) {
        int total = 0;

        for (int offset = 0; offset < days; offset++) {
            DailyArticleDemand demand = series.days().get(endDate.minusDays(offset));
            total += demand == null ? 0 : demand.orderCount();
        }

        return total;
    }

    private long daysSinceDemand(ArticleSeries series, LocalDate featureDate) {
        return series.days().keySet().stream()
                .filter(date -> !date.isAfter(featureDate))
                .max(LocalDate::compareTo)
                .map(date -> ChronoUnit.DAYS.between(date, featureDate))
                .orElse((long) properties.getMaximumLookbackDays());
    }

    private int quantityOn(ArticleSeries series, LocalDate date) {
        DailyArticleDemand demand = series.days().get(date);
        return demand == null ? 0 : demand.quantity();
    }

    private boolean isValid(DemandObservation observation) {
        return observation != null
                && observation.articleId() != null
                && observation.orderId() != null
                && observation.orderedAt() != null
                && observation.quantity() > 0;
    }

    private void validateConfiguration() {
        if (properties.getForecastHorizonDays() <= 0
                || properties.getValidationDays() <= properties.getForecastHorizonDays()
                || properties.getMaximumLookbackDays() <= 0
                || properties.getMinimumArticleHistoryDays() < 28) {
            throw new IllegalStateException("Invalid demand forecast dataset configuration");
        }
    }

    private record ArticleSeries(
            Long articleId,
            LocalDate firstDemandDate,
            Map<LocalDate, DailyArticleDemand> days
    ) {
    }

    private static final class MutableDailyDemand {
        private int quantity;
        private final Set<Long> orderIds = new HashSet<>();
    }
}
