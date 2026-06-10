package com.nikitaopara.warehouseoptimizer.demand.forecast.repository;

import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModel;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastModelStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DemandForecastModelRepository extends JpaRepository<DemandForecastModel, Long> {

    @EntityGraph(attributePaths = "warehouse")
    Optional<DemandForecastModel> findFirstByWarehouseIdAndStatusOrderByTrainedAtDesc(
            Long warehouseId,
            DemandForecastModelStatus status
    );

    Optional<DemandForecastModel> findFirstByWarehouseIdOrderByVersionNumberDesc(Long warehouseId);

    List<DemandForecastModel> findByWarehouseIdOrderByVersionNumberDesc(Long warehouseId);
}
