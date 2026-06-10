package com.nikitaopara.warehouseoptimizer.optimization.controller;

import com.nikitaopara.warehouseoptimizer.optimization.dto.WarehouseOptimizationAssessmentResponse;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentTrigger;
import com.nikitaopara.warehouseoptimizer.optimization.service.WarehouseOptimizationAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/warehouses/{warehouseId}/optimization-assessments")
@RequiredArgsConstructor
public class WarehouseOptimizationAssessmentController {

    private final WarehouseOptimizationAssessmentService assessmentService;

    @PostMapping
    public ResponseEntity<WarehouseOptimizationAssessmentResponse> analyzeWarehouse(
            @PathVariable Long warehouseId
    ) {
        return ResponseEntity.ok(assessmentService.analyzeWarehouse(
                warehouseId,
                OptimizationAssessmentTrigger.MANUAL
        ));
    }

    @GetMapping("/latest")
    public ResponseEntity<WarehouseOptimizationAssessmentResponse> getLatestAssessment(
            @PathVariable Long warehouseId
    ) {
        return ResponseEntity.ok(assessmentService.getLatestAssessment(warehouseId));
    }
}
