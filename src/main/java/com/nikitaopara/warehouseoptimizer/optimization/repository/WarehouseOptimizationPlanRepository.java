package com.nikitaopara.warehouseoptimizer.optimization.repository;

import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationPlanStatus;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from WarehouseOptimizationPlan plan where plan.code = :code")
    Optional<WarehouseOptimizationPlan> findForUpdateByCode(@Param("code") String code);

    boolean existsByWarehouseIdAndStatusIn(
            Long warehouseId,
            Collection<OptimizationPlanStatus> statuses
    );
}
