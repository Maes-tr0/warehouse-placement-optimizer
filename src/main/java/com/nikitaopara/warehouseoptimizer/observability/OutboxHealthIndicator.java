package com.nikitaopara.warehouseoptimizer.observability;

import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEventStatus;
import com.nikitaopara.warehouseoptimizer.eventing.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("outbox")
@RequiredArgsConstructor
public class OutboxHealthIndicator extends AbstractHealthIndicator {

    private static final long DEGRADED_FAILED_EVENT_COUNT = 100;

    private final OutboxEventRepository repository;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        long pending = repository.countByStatusIn(List.of(
                OutboxEventStatus.PENDING,
                OutboxEventStatus.PROCESSING
        ));
        long failed = repository.countByStatusIn(List.of(OutboxEventStatus.FAILED));
        if (failed >= DEGRADED_FAILED_EVENT_COUNT) {
            builder.status("DEGRADED");
        } else {
            builder.up();
        }
        builder.withDetail("pendingEvents", pending)
                .withDetail("failedEvents", failed);
    }
}
