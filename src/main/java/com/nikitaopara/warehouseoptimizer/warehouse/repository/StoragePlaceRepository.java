package com.nikitaopara.warehouseoptimizer.warehouse.repository;

import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface StoragePlaceRepository extends JpaRepository<StoragePlace, Long> {
    Optional<StoragePlace> findStoragePlaceByWarehouseIdAndCode(Long warehouseId, String code);

    @EntityGraph(attributePaths = {"warehouse", "rackRow", "rackBay", "rackLevel"})
    List<StoragePlace> findByWarehouseIdAndStatusOrderByDistanceFromEntryMmAsc(
            Long warehouseId,
            StoragePlaceStatus status
    );

    @EntityGraph(attributePaths = {"warehouse", "rackRow", "rackBay", "rackLevel"})
    List<StoragePlace> findByWarehouseIdOrderByDistanceFromEntryMmAsc(Long warehouseId);
}
