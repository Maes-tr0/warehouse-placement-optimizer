package com.nikitaopara.warehouseoptimizer.warehouse.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.StoragePlaceResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseSummaryResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final AuthenticatedUserService authenticatedUserService;
    private final WarehouseValidationService warehouseValidationService;
    private final WarehouseGenerationService warehouseGenerationService;
    private final WarehouseDataService warehouseDataService;
    private final WarehouseRepository warehouseRepository;
    private final StoragePlaceRepository storagePlaceRepository;

    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        warehouseValidationService.validateCreateWarehouseRequest(actor, request);

        Warehouse warehouse = warehouseGenerationService.generate(actor, request);

        Warehouse savedWarehouse = warehouseDataService.save(warehouse);

        return WarehouseResponse.from(savedWarehouse);
    }

    @Transactional(readOnly = true)
    public List<WarehouseSummaryResponse> getWarehouses() {
        return warehouseRepository.findAllSummaries();
    }

    @Transactional(readOnly = true)
    public WarehouseSummaryResponse getWarehouse(Long warehouseId) {
        return warehouseRepository.findSummaryById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found: " + warehouseId
                ));
    }

    @Transactional(readOnly = true)
    public List<StoragePlaceResponse> getStoragePlaces(
            Long warehouseId,
            StoragePlaceStatus status
    ) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found: " + warehouseId);
        }

        var places = status == null
                ? storagePlaceRepository.findByWarehouseIdOrderByDistanceFromEntryMmAsc(warehouseId)
                : storagePlaceRepository.findByWarehouseIdAndStatusOrderByDistanceFromEntryMmAsc(
                        warehouseId,
                        status
                );

        return places.stream().map(StoragePlaceResponse::from).toList();
    }
}
