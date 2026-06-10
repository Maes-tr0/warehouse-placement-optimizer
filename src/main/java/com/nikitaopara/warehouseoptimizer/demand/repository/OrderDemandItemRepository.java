package com.nikitaopara.warehouseoptimizer.demand.repository;

import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemandItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDemandItemRepository extends JpaRepository<OrderDemandItem, Long> {

    List<OrderDemandItem> findByWarehouseIdAndArticleId(
            Long warehouseId,
            Long articleId
    );

    List<OrderDemandItem> findByWarehouseIdAndArticleIdAndOrderDemandOrderDateTimeBetween(
            Long warehouseId,
            Long articleId,
            LocalDateTime from,
            LocalDateTime to
    );

    @EntityGraph(attributePaths = {"article", "orderDemand"})
    List<OrderDemandItem> findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
            Long warehouseId,
            LocalDateTime from,
            LocalDateTime to
    );
}
