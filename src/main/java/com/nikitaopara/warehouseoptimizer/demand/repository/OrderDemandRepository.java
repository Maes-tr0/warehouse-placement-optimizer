package com.nikitaopara.warehouseoptimizer.demand.repository;

import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderDemandRepository extends JpaRepository<OrderDemand, Long> {

    boolean existsByWarehouseIdAndOrderNumber(
            Long warehouseId,
            String orderNumber
    );

    Optional<OrderDemand> findByWarehouseIdAndOrderNumber(
            Long warehouseId,
            String orderNumber
    );

    List<OrderDemand> findByWarehouseIdAndOrderNumberIn(
            Long warehouseId,
            Collection<String> orderNumbers
    );

    List<OrderDemand> findByWarehouseIdAndOrderDateTimeBetween(
            Long warehouseId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<OrderDemand> findByWarehouseIdOrderByOrderDateTimeDesc(Long warehouseId);
}