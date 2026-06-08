package com.nikitaopara.warehouseoptimizer.warehouse.service;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseDataService {

    private final WarehouseRepository warehouseRepository;

    public boolean existsByCode(String code) {
        return warehouseRepository.existsByCode(code);
    }

    public Warehouse save(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }
}