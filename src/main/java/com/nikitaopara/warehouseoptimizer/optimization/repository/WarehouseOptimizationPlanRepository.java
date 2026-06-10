package com.nikitaopara.warehouseoptimizer.optimization.repository;

import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationPlanStatus;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface WarehouseOptimizationPlanRepository
        extends JpaRepository<WarehouseOptimizationPlan, Long> {

    boolean existsByAssessmentId(Long assessmentId);

    @EntityGraph(attributePaths = {
            "warehouse",
            "assessment",
            "steps",
            "steps.sourceContainer",
            "steps.sourceContainer.article",
            "steps.targetContainer",
            "steps.targetContainer.article",
            "steps.fromStoragePlace",
            "steps.toStoragePlace"
    })
    Optional<WarehouseOptimizationPlan> findByCode(String code);

    boolean existsByWarehouseIdAndStatusIn(
            Long warehouseId,
            Collection<OptimizationPlanStatus> statuses
    );
}
