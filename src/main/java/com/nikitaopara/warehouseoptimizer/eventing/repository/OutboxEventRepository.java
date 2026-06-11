package com.nikitaopara.warehouseoptimizer.eventing.repository;

import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEvent;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    long countByStatusIn(List<OutboxEventStatus> statuses);

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status IN ('PENDING', 'FAILED')
              AND available_at <= :availableAt
            ORDER BY occurred_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<OutboxEvent> findDispatchableForUpdate(
            @Param("availableAt") Instant availableAt,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query("""
            UPDATE OutboxEvent event
            SET event.status = com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEventStatus.FAILED,
                event.lastError = 'Dispatcher lease expired'
            WHERE event.status = com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEventStatus.PROCESSING
              AND event.availableAt <= :now
            """)
    int recoverExpiredProcessingLeases(@Param("now") Instant now);
}
