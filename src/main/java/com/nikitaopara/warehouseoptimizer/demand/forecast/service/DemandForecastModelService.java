package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.demand.forecast.dto.DemandForecastModelResponse;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModel;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastTrainingTrigger;
import com.nikitaopara.warehouseoptimizer.demand.forecast.repository.DemandForecastModelRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandForecastModelService {

    private final WarehouseRepository warehouseRepository;
    private final DemandForecastModelRepository modelRepository;
    private final DemandForecastTrainingService trainingService;

    @Transactional
    public DemandForecastModelResponse train(Long warehouseId) {
        String modelCode = trainingService.train(
                warehouseId,
                DemandForecastTrainingTrigger.MANUAL
        ).modelCode();
        DemandForecastModel model = modelRepository.findByCode(modelCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Trained demand forecast model was not persisted: " + modelCode
                ));

        return toResponse(model);
    }

    @Transactional(readOnly = true)
    public DemandForecastModelResponse getLatest(Long warehouseId) {
        validateWarehouse(warehouseId);
        return modelRepository.findFirstByWarehouseIdOrderByVersionNumberDesc(warehouseId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demand forecast model not found for warehouse: " + warehouseId
                ));
    }

    @Transactional(readOnly = true)
    public List<DemandForecastModelResponse> getHistory(Long warehouseId) {
        validateWarehouse(warehouseId);
        return modelRepository.findByWarehouseIdOrderByVersionNumberDesc(warehouseId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateWarehouse(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found: " + warehouseId);
        }
    }

    private DemandForecastModelResponse toResponse(DemandForecastModel model) {
        return new DemandForecastModelResponse(
                model.getCode(),
                model.getWarehouse().getId(),
                model.getVersionNumber(),
                model.getStatus(),
                model.getTrainingTrigger(),
                model.getAlgorithm(),
                model.getFeatureSchemaVersion(),
                model.getForecastHorizonDays(),
                model.getTrainingStart(),
                model.getTrainingEnd(),
                model.getValidationStart(),
                model.getValidationEnd(),
                model.getDataCutoff(),
                model.getObservationCount(),
                model.getArticleCount(),
                model.getTrainingSampleCount(),
                model.getValidationSampleCount(),
                model.getModelMae(),
                model.getBaselineMae(),
                model.getModelRmse(),
                model.getModelR2(),
                model.getImprovementPercent(),
                model.getErrorMessage(),
                model.getTrainedAt()
        );
    }
}
