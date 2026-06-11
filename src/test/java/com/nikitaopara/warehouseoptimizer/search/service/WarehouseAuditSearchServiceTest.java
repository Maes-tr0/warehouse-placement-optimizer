package com.nikitaopara.warehouseoptimizer.search.service;

import com.nikitaopara.warehouseoptimizer.search.config.SearchProperties;
import com.nikitaopara.warehouseoptimizer.search.model.WarehouseAuditEventDocument;
import com.nikitaopara.warehouseoptimizer.search.repository.WarehouseAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseAuditSearchServiceTest {

    @Mock
    private WarehouseAuditEventRepository repository;

    @Test
    void filtersWarehouseEventsByType() {
        SearchProperties properties = new SearchProperties();
        properties.setEnabled(true);
        WarehouseAuditSearchService service = new WarehouseAuditSearchService(
                repository,
                properties
        );
        var pageable = PageRequest.of(0, 20);
        var document = new WarehouseAuditEventDocument(
                "event-1",
                "warehouse.optimization.v1",
                "warehouse.optimization.assessed",
                "WarehouseOptimizationAssessment",
                "10",
                "WH-1",
                Instant.parse("2026-06-11T03:00:00Z"),
                Map.of("scorePercent", 55)
        );
        when(repository.findByWarehouseKeyAndEventTypeOrderByOccurredAtDesc(
                "WH-1",
                "warehouse.optimization.assessed",
                pageable
        )).thenReturn(new PageImpl<>(List.of(document), pageable, 1));

        var result = service.search(
                "WH-1",
                "warehouse.optimization.assessed",
                0,
                20
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().eventId()).isEqualTo("event-1");
        assertThat(result.totalElements()).isEqualTo(1);
        verify(repository).findByWarehouseKeyAndEventTypeOrderByOccurredAtDesc(
                "WH-1",
                "warehouse.optimization.assessed",
                pageable
        );
    }

    @Test
    void rejectsSearchWhenFeatureIsDisabled() {
        WarehouseAuditSearchService service = new WarehouseAuditSearchService(
                repository,
                new SearchProperties()
        );

        assertThatThrownBy(() -> service.search("WH-1", null, 0, 20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }
}
