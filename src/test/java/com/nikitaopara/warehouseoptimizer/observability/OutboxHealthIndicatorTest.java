package com.nikitaopara.warehouseoptimizer.observability;

import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEventStatus;
import com.nikitaopara.warehouseoptimizer.eventing.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxHealthIndicatorTest {

    @Mock
    private OutboxEventRepository repository;

    @Test
    void reportsDegradedWhenFailedBacklogIsLarge() {
        when(repository.countByStatusIn(List.of(
                OutboxEventStatus.PENDING,
                OutboxEventStatus.PROCESSING
        ))).thenReturn(5L);
        when(repository.countByStatusIn(List.of(OutboxEventStatus.FAILED))).thenReturn(100L);

        var health = new OutboxHealthIndicator(repository).health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails()).containsEntry("pendingEvents", 5L);
        assertThat(health.getDetails()).containsEntry("failedEvents", 100L);
    }
}
