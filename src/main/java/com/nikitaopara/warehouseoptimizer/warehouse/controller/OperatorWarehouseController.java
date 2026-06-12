package com.nikitaopara.warehouseoptimizer.warehouse.controller;

import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseSummaryResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Warehouses", description = "Warehouse list and warehouse access for operators")
@RestController
@RequestMapping("/operator/warehouses")
@RequiredArgsConstructor
public class OperatorWarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public List<WarehouseSummaryResponse> getWarehouses() {
        return warehouseService.getWarehouses();
    }
}
