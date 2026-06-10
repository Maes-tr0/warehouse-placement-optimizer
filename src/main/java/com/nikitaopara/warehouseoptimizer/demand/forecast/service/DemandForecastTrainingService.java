package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.*;
import com.nikitaopara.warehouseoptimizer.demand.forecast.repository.DemandForecastModelRepository;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemandItem;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import com.nikitaopara.warehouseoptimizer.optimization.model.DemandObservation;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandForecastTrainingService {

    private static final String ALGORITHM = "TRIBUO_CART_REGRESSION";
    private static final int FEATURE_SCHEMA_VERSION = 1;

    private final WarehouseRepository warehouseRepository;
    private final OrderDemandItemRepository orderDemandItemRepository;
    private final DemandForecastModelRepository modelRepository;
    private final DemandForecastDatasetBuilder datasetBuilder;
    private final TribuoDemandForecastTrainer trainer;
    private final DemandForecastProperties properties;

    @Transactional
    public DemandForecastTrainingResult train(
            Long warehouseId,
            DemandForecastTrainingTrigger trigger
    ) {
        return train(warehouseId, trigger, LocalDate.now());
    }

    @Transactional
    DemandForecastTrainingResult train(
            Long warehouseId,
            DemandForecastTrainingTrigger trigger,
            LocalDate dataCutoff
    ) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Demand forecast training is disabled");
        }

        Warehouse warehouse = warehouseRepository.findByIdForUpdate(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found: " + warehouseId
                ));
        LocalDate earliestDate = dataCutoff.minusDays(properties.getMaximumLookbackDays());
        List<OrderDemandItem> items = orderDemandItemRepository
                .findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
                        warehouseId,
                        earliestDate.atStartOfDay(),
                        dataCutoff.atTime(LocalTime.MAX)
                );
        DemandForecastDataset dataset = datasetBuilder.build(
                items.stream().map(this::toObservation).toList(),
                dataCutoff
        );

        if (!dataset.hasEnoughSamples(
                properties.getMinimumTrainingSamples(),
                properties.getMinimumValidationSamples()
        )) {
            throw new IllegalArgumentException(
                    "Insufficient forecast samples: training="
                            + dataset.trainingRows().size()
                            + ", validation="
                            + dataset.validationRows().size()
            );
        }

        int versionNumber = modelRepository
                .findFirstByWarehouseIdOrderByVersionNumberDesc(warehouseId)
                .map(model -> model.getVersionNumber() + 1)
                .orElse(1);
        DemandForecastModel modelVersion = createTrainingModel(
                warehouse,
                trigger,
                versionNumber,
                dataset
        );
        modelRepository.saveAndFlush(modelVersion);

        try {
            TrainedDemandForecast trained = trainer.train(dataset);
            applyMetrics(modelVersion, trained.metrics());
            LocalDateTime completedAt = LocalDateTime.now();

            if (shouldActivate(trained.metrics())) {
                modelRepository.findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(
                                warehouseId,
                                DemandForecastModelStatus.ACTIVE
                        )
                        .ifPresent(active -> {
                            active.supersede();
                            modelRepository.saveAndFlush(active);
                        });
                modelVersion.activate(trained.artifact(), completedAt);
            } else {
                modelVersion.reject(
                        "Candidate did not improve baseline by at least "
                                + properties.getMinimumImprovementPercent()
                                + "%",
                        completedAt
                );
            }
        } catch (RuntimeException exception) {
            modelVersion.fail(trimMessage(exception), LocalDateTime.now());
        }

        return toResult(modelRepository.save(modelVersion));
    }

    private DemandForecastModel createTrainingModel(
            Warehouse warehouse,
            DemandForecastTrainingTrigger trigger,
            int versionNumber,
            DemandForecastDataset dataset
    ) {
        return DemandForecastModel.builder()
                .code("DFM-" + UUID.randomUUID())
                .warehouse(warehouse)
                .versionNumber(versionNumber)
                .status(DemandForecastModelStatus.TRAINING)
                .trainingTrigger(trigger)
                .algorithm(ALGORITHM)
                .featureSchemaVersion(FEATURE_SCHEMA_VERSION)
                .forecastHorizonDays(properties.getForecastHorizonDays())
                .trainingStart(dataset.dataStart())
                .trainingEnd(dataset.trainingRows().getLast().featureDate())
                .validationStart(dataset.validationRows().getFirst().featureDate())
                .validationEnd(dataset.validationRows().getLast().featureDate())
                .dataCutoff(dataset.dataCutoff())
                .observationCount(dataset.observationCount())
                .articleCount(dataset.articleCount())
                .trainingSampleCount(dataset.trainingRows().size())
                .validationSampleCount(dataset.validationRows().size())
                .build();
    }

    private void applyMetrics(DemandForecastModel model, DemandForecastMetrics metrics) {
        model.setModelMae(metrics.modelMae());
        model.setBaselineMae(metrics.baselineMae());
        model.setModelRmse(metrics.modelRmse());
        model.setModelR2(metrics.modelR2());
        model.setImprovementPercent(metrics.improvementPercent());
    }

    private boolean shouldActivate(DemandForecastMetrics metrics) {
        if (metrics.baselineMae() == 0.0) {
            return metrics.modelMae() == 0.0;
        }

        return metrics.improvementPercent() >= properties.getMinimumImprovementPercent();
    }

    private DemandObservation toObservation(OrderDemandItem item) {
        return new DemandObservation(
                item.getArticle().getId(),
                item.getOrderDemand().getId(),
                item.getOrderDemand().getOrderDateTime(),
                item.getQuantity()
        );
    }

    private DemandForecastTrainingResult toResult(DemandForecastModel model) {
        DemandForecastMetrics metrics = model.getModelMae() == null
                ? null
                : new DemandForecastMetrics(
                        model.getModelMae(),
                        model.getBaselineMae(),
                        model.getModelRmse(),
                        model.getModelR2(),
                        model.getImprovementPercent()
                );

        return new DemandForecastTrainingResult(
                model.getCode(),
                model.getVersionNumber(),
                model.getStatus(),
                model.getObservationCount(),
                model.getArticleCount(),
                model.getTrainingSampleCount(),
                model.getValidationSampleCount(),
                metrics,
                model.getErrorMessage()
        );
    }

    private String trimMessage(RuntimeException exception) {
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
