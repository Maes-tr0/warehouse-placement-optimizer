package com.nikitaopara.warehouseoptimizer.warehouse.repository;

import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoragePlaceRepository extends JpaRepository<StoragePlace, Long> {
    Optional<StoragePlace> findStoragePlaceByWarehouseIdAndCode(Long warehouseId, String code);
}
