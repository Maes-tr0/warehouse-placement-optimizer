package com.nikitaopara.warehouseoptimizer.warehouse.service;

import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.WarehouseResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class WarehouseService {
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request, Authentication authentication) {
        return null;
    }
}
