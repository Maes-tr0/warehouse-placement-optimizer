package com.nikitaopara.warehouseoptimizer.movement.repository;

import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContainerMovementRepository extends JpaRepository<ContainerMovement, Long> {

    @EntityGraph(attributePaths = {
            "warehouse",
            "container",
            "targetContainer",
            "optimizationPlan",
            "relocationStep",
            "performedBy"
    })
    List<ContainerMovement> findByWarehouseIdOrderByPerformedAtDesc(Long warehouseId);

    @EntityGraph(attributePaths = {
            "warehouse",
            "container",
            "targetContainer",
            "optimizationPlan",
            "relocationStep",
            "performedBy"
    })
    List<ContainerMovement> findByContainerContainerNumberOrderByPerformedAtDesc(
            String containerNumber
    );
}
