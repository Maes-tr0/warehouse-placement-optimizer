package com.nikitaopara.warehouseoptimizer.warehouse.repository;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoragePlaceRepository extends JpaRepository<Warehouse, Long> {

}
