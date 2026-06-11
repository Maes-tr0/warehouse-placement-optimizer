package com.nikitaopara.warehouseoptimizer.optimization.repository;

import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseOptimizationAssessmentRepository
        extends JpaRepository<WarehouseOptimizationAssessment, Long> {

    Optional<WarehouseOptimizationAssessment> findFirstByWarehouseIdOrderByAnalyzedAtDesc(Long warehouseId);
}
