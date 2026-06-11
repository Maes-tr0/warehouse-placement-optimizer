package com.nikitaopara.warehouseoptimizer.warehouse.controller;

import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.StoragePlaceResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseSummaryResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.dto.WarehouseRouteResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.service.WarehouseRoutingService;
import com.nikitaopara.warehouseoptimizer.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final WarehouseRoutingService warehouseRoutingService;

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(@RequestBody CreateWarehouseRequest request) {
        WarehouseResponse response = warehouseService.createWarehouse(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<WarehouseSummaryResponse> getWarehouses() {
        return warehouseService.getWarehouses();
    }

    @GetMapping("/{warehouseId}")
    public WarehouseSummaryResponse getWarehouse(@PathVariable Long warehouseId) {
        return warehouseService.getWarehouse(warehouseId);
    }

    @GetMapping("/{warehouseId}/storage-places")
    public List<StoragePlaceResponse> getStoragePlaces(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) StoragePlaceStatus status
    ) {
        return warehouseService.getStoragePlaces(warehouseId, status);
    }

    @GetMapping("/{warehouseId}/routes/storage-places/{storagePlaceCode}")
    public WarehouseRouteResponse getStoragePlaceRoute(
            @PathVariable Long warehouseId,
            @PathVariable String storagePlaceCode
    ) {
        return warehouseRoutingService.getRoute(warehouseId, storagePlaceCode);
    }
}
