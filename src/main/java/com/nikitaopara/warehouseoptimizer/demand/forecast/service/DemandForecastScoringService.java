package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModel;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModelStatus;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastRow;
import com.nikitaopara.warehouseoptimizer.demand.forecast.repository.DemandForecastModelRepository;
import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;
import com.nikitaopara.warehouseoptimizer.optimization.model.DemandObservation;
import com.nikitaopara.warehouseoptimizer.optimization.service.SeasonalDemandModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tribuo.Model;
import org.tribuo.regression.Regressor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandForecastScoringService {

    private static final Logger log = LoggerFactory.getLogger(DemandForecastScoringService.class);
    private static final int SUPPORTED_FEATURE_SCHEMA_VERSION = 1;

    private final DemandForecastModelRepository modelRepository;
    private final DemandForecastDatasetBuilder datasetBuilder;
    private final TribuoDemandForecastTrainer trainer;
    private final SeasonalDemandModel seasonalDemandModel;
    private final DemandForecastProperties forecastProperties;
    private final OptimizationProperties optimizationProperties;
    private final Map<String, Model<Regressor>> modelCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Map<Long, ArticleDemandScore> calculate(
            Long warehouseId,
            List<DemandObservation> observations,
            LocalDate analysisDate
    ) {
        Map<Long, ArticleDemandScore> fallbackScores = seasonalDemandModel.calculate(
                observations,
                analysisDate,
                optimizationProperties.getRecencyHalfLifeDays(),
                optimizationProperties.getSeasonalWindowDays()
        );

        if (!forecastProperties.isEnabled() || fallbackScores.isEmpty()) {
            return fallbackScores;
        }

        DemandForecastModel activeModel = modelRepository
                .findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(
                        warehouseId,
                        DemandForecastModelStatus.ACTIVE
                )
                .orElse(null);

        if (!isUsable(activeModel)) {
            return fallbackScores;
        }

        try {
            Model<Regressor> model = modelCache.computeIfAbsent(
                    activeModel.getCode(),
                    ignored -> trainer.deserialize(activeModel.getModelArtifact())
            );
            Map<Long, List<DemandObservation>> observationsByArticle = observations.stream()
                    .collect(Collectors.groupingBy(DemandObservation::articleId));
            Map<Long, ArticleDemandScore> scores = new HashMap<>(fallbackScores);

            observationsByArticle.forEach((articleId, articleObservations) -> {
                ArticleDemandScore fallback = fallbackScores.get(articleId);

                if (fallback == null) {
                    return;
                }

                try {
                    DemandForecastRow row = datasetBuilder.buildPredictionRow(
                            articleId,
                            articleObservations,
                            analysisDate
                    );
                    double forecastQuantity = trainer.predict(model, row);
                    scores.put(articleId, new ArticleDemandScore(
                            articleId,
                            forecastQuantity,
                            fallback.totalQuantity(),
                            fallback.orderCount()
                    ));
                } catch (IllegalArgumentException exception) {
                    log.debug(
                            "Using seasonal demand fallback for warehouse {}, article {}: {}",
                            warehouseId,
                            articleId,
                            exception.getMessage()
                    );
                }
            });

            return Map.copyOf(scores);
        } catch (RuntimeException exception) {
            log.error(
                    "Cannot use active demand forecast model {} for warehouse {}; using fallback",
                    activeModel.getCode(),
                    warehouseId,
                    exception
            );
            return fallbackScores;
        }
    }

    private boolean isUsable(DemandForecastModel model) {
        return model != null
                && model.getFeatureSchemaVersion() == SUPPORTED_FEATURE_SCHEMA_VERSION
                && model.getForecastHorizonDays() == forecastProperties.getForecastHorizonDays()
                && model.getModelArtifact() != null
                && model.getModelArtifact().length > 0;
    }
}
