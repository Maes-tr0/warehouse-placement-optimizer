package com.nikitaopara.warehouseoptimizer.optimization.controller;

import com.nikitaopara.warehouseoptimizer.optimization.dto.WarehouseOptimizationPlanResponse;
import com.nikitaopara.warehouseoptimizer.optimization.service.WarehouseOptimizationPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/optimization-plans")
@RequiredArgsConstructor
public class WarehouseOptimizationPlanController {

    private final WarehouseOptimizationPlanService planService;

    @PostMapping("/assessments/{assessmentId}")
    public ResponseEntity<WarehouseOptimizationPlanResponse> createPlan(
            @PathVariable Long assessmentId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.createPlan(assessmentId));
    }

    @GetMapping("/{planCode}")
    public ResponseEntity<WarehouseOptimizationPlanResponse> getPlan(
            @PathVariable String planCode
    ) {
        return ResponseEntity.ok(planService.getPlan(planCode));
    }

    @PostMapping("/{planCode}/approve")
    public ResponseEntity<WarehouseOptimizationPlanResponse> approvePlan(
            @PathVariable String planCode
    ) {
        return ResponseEntity.ok(planService.approvePlan(planCode));
    }

    @PostMapping("/{planCode}/cancel")
    public ResponseEntity<WarehouseOptimizationPlanResponse> cancelPlan(
            @PathVariable String planCode
    ) {
        return ResponseEntity.ok(planService.cancelPlan(planCode));
    }
}
