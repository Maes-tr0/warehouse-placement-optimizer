package com.nikitaopara.warehouseoptimizer.warehouse.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final AuthenticatedUserService authenticatedUserService;
    private final WarehouseValidationService warehouseValidationService;
    private final WarehouseGenerationService warehouseGenerationService;
    private final WarehouseDataService warehouseDataService;

    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        warehouseValidationService.validateCreateWarehouseRequest(actor, request);

        Warehouse warehouse = warehouseGenerationService.generate(actor, request);

        Warehouse savedWarehouse = warehouseDataService.save(warehouse);

        return WarehouseResponse.from(savedWarehouse);
    }
}
