package com.nikitaopara.warehouseoptimizer.warehouse.controller;

import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(@RequestBody CreateWarehouseRequest request) {
        WarehouseResponse response = warehouseService.createWarehouse(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}