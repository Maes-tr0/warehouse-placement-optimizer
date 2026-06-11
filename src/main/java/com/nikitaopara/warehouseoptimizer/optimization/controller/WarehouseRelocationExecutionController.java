package com.nikitaopara.warehouseoptimizer.optimization.controller;

import com.nikitaopara.warehouseoptimizer.optimization.dto.CompleteRelocationStepRequest;
import com.nikitaopara.warehouseoptimizer.optimization.dto.RelocationExecutionResponse;
import com.nikitaopara.warehouseoptimizer.optimization.dto.RelocationStepResponse;
import com.nikitaopara.warehouseoptimizer.optimization.service.WarehouseRelocationExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operator/optimization-plans/{planCode}/steps")
@RequiredArgsConstructor
public class WarehouseRelocationExecutionController {

    private final WarehouseRelocationExecutionService executionService;

    @GetMapping("/current")
    public ResponseEntity<RelocationStepResponse> getCurrentStep(
            @PathVariable String planCode
    ) {
        return ResponseEntity.ok(executionService.getCurrentStep(planCode));
    }

    @PostMapping("/current/complete")
    public ResponseEntity<RelocationExecutionResponse> completeCurrentStep(
            @PathVariable String planCode,
            @RequestBody CompleteRelocationStepRequest request
    ) {
        return ResponseEntity.ok(executionService.completeCurrentStep(planCode, request));
    }
}
