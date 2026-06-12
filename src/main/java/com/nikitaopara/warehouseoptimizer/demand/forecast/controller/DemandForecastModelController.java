package com.nikitaopara.warehouseoptimizer.demand.forecast.controller;

import com.nikitaopara.warehouseoptimizer.demand.forecast.dto.DemandForecastModelResponse;
import com.nikitaopara.warehouseoptimizer.demand.forecast.service.DemandForecastModelService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Demand Forecast Models", description = "AI and ML demand forecast model training and history")
@RestController
@RequestMapping("/admin/warehouses/{warehouseId}/demand-forecast-models")
@RequiredArgsConstructor
public class DemandForecastModelController {

    private final DemandForecastModelService modelService;

    @PostMapping("/train")
    public ResponseEntity<DemandForecastModelResponse> train(
            @PathVariable Long warehouseId
    ) {
        return ResponseEntity.ok(modelService.train(warehouseId));
    }

    @GetMapping("/latest")
    public ResponseEntity<DemandForecastModelResponse> getLatest(
            @PathVariable Long warehouseId
    ) {
        return ResponseEntity.ok(modelService.getLatest(warehouseId));
    }

    @GetMapping
    public ResponseEntity<List<DemandForecastModelResponse>> getHistory(
            @PathVariable Long warehouseId
    ) {
        return ResponseEntity.ok(modelService.getHistory(warehouseId));
    }
}
