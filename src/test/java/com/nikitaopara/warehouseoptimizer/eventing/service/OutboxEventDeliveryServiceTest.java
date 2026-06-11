package com.nikitaopara.warehouseoptimizer.eventing.service;

import com.nikitaopara.warehouseoptimizer.eventing.config.EventingProperties;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEvent;
import com.nikitaopara.warehouseoptimizer.eventing.model.OutboxEventStatus;
import com.nikitaopara.warehouseoptimizer.eventing.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventDeliveryServiceTest {

    @Mock
    private OutboxEventRepository repository;

    @Test
    void claimsAvailableEventsAndRecoversExpiredLeasesFirst() {
        EventingProperties properties = new EventingProperties();
        properties.setDispatchBatchSize(25);
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .availableAt(Instant.now())
                .build();
        when(repository.findDispatchableForUpdate(any(), eq(25))).thenReturn(List.of(event));
        OutboxEventDeliveryService service = new OutboxEventDeliveryService(repository, properties);

        var claimed = service.claimBatch();

        verify(repository).recoverExpiredProcessingLeases(any());
        assertThat(claimed).containsExactly(event);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getAvailableAt()).isAfter(Instant.now());
    }
}
