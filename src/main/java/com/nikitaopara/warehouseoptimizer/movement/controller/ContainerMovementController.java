package com.nikitaopara.warehouseoptimizer.movement.controller;

import com.nikitaopara.warehouseoptimizer.movement.dto.ContainerMovementResponse;
import com.nikitaopara.warehouseoptimizer.movement.service.ContainerMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/container-movements")
@RequiredArgsConstructor
public class ContainerMovementController {

    private final ContainerMovementService movementService;

    @GetMapping
    public ResponseEntity<List<ContainerMovementResponse>> getWarehouseHistory(
            @RequestParam Long warehouseId
    ) {
        return ResponseEntity.ok(movementService.getWarehouseHistory(warehouseId)
                .stream()
                .map(ContainerMovementResponse::from)
                .toList());
    }

    @GetMapping("/containers/{containerNumber}")
    public ResponseEntity<List<ContainerMovementResponse>> getContainerHistory(
            @PathVariable String containerNumber
    ) {
        return ResponseEntity.ok(movementService.getContainerHistory(containerNumber)
                .stream()
                .map(ContainerMovementResponse::from)
                .toList());
    }
}
