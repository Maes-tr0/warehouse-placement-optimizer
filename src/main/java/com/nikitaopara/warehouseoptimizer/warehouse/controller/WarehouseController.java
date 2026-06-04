package com.nikitaopara.warehouseoptimizer.warehouse.controller;

import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @RequestBody CreateWarehouseRequest request,
            Authentication authentication
    ) {
        WarehouseResponse response = warehouseService.createWarehouse(request, authentication);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}