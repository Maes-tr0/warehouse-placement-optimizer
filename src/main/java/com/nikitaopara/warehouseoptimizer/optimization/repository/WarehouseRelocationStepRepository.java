package com.nikitaopara.warehouseoptimizer.optimization.repository;

import com.nikitaopara.warehouseoptimizer.optimization.model.RelocationStepStatus;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseRelocationStep;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseRelocationStepRepository
        extends JpaRepository<WarehouseRelocationStep, Long> {

    @EntityGraph(attributePaths = {
            "plan",
            "plan.warehouse",
            "sourceContainer",
            "sourceContainer.article",
            "sourceContainer.currentStoragePlace",
            "targetContainer",
            "targetContainer.article",
            "targetContainer.currentStoragePlace",
            "fromStoragePlace",
            "toStoragePlace"
    })
    Optional<WarehouseRelocationStep> findByPlanCodeAndStatus(
            String planCode,
            RelocationStepStatus status
    );
}
