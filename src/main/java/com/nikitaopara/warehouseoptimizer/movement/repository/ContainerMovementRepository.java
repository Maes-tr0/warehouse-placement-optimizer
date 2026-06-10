package com.nikitaopara.warehouseoptimizer.movement.repository;

import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContainerMovementRepository extends JpaRepository<ContainerMovement, Long> {

    List<ContainerMovement> findByWarehouseIdOrderByPerformedAtDesc(Long warehouseId);

    List<ContainerMovement> findByContainerContainerNumberOrderByPerformedAtDesc(
            String containerNumber
    );
}
