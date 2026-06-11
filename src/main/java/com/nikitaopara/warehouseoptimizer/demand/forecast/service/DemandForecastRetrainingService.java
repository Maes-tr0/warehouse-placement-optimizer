package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModel;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModelStatus;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastTrainingResult;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastTrainingTrigger;
import com.nikitaopara.warehouseoptimizer.demand.forecast.repository.DemandForecastModelRepository;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DemandForecastRetrainingService {

    private final DemandForecastModelRepository modelRepository;
    private final OrderDemandItemRepository orderDemandItemRepository;
    private final DemandForecastRetrainingPolicy policy;
    private final DemandForecastTrainingService trainingService;

    public Optional<DemandForecastTrainingResult> trainIfNeeded(
            Long warehouseId,
            LocalDate today
    ) {
        DemandForecastModel activeModel = modelRepository
                .findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(
                        warehouseId,
                        DemandForecastModelStatus.ACTIVE
                )
                .orElse(null);
        DemandForecastModel latestAttempt = modelRepository
                .findFirstByWarehouseIdOrderByVersionNumberDesc(warehouseId)
                .orElse(null);
        long newObservationCount = activeModel == null
                ? 0
                : orderDemandItemRepository
                .countByWarehouseIdAndCreatedAtAfter(
                        warehouseId,
                        activeModel.getTrainedAt()
                );

        if (!policy.shouldRetrain(activeModel, latestAttempt, newObservationCount, today)) {
            return Optional.empty();
        }

        return Optional.of(trainingService.train(
                warehouseId,
                DemandForecastTrainingTrigger.SCHEDULED,
                today.minusDays(1)
        ));
    }
}
