package com.nikitaopara.warehouseoptimizer.demand.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastScore;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandScoreSource;
import com.nikitaopara.warehouseoptimizer.demand.forecast.service.DemandForecastScoringService;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemand;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemandItem;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.ArticleDemandScore;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandAnalyticsServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private OrderDemandItemRepository orderDemandItemRepository;
    @Mock
    private ContainerRepository containerRepository;
    @Mock
    private DemandForecastScoringService scoringService;

    private DemandAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new DemandAnalyticsService(
                warehouseRepository,
                orderDemandItemRepository,
                containerRepository,
                scoringService,
                new OptimizationProperties()
        );
    }

    @Test
    void ranksDemandAndAddsInventoryDistanceContext() {
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-1").build();
        Article popular = Article.builder().id(10L).articleNumber("100").name("Popular").build();
        Article slow = Article.builder().id(20L).articleNumber("200").name("Slow").build();
        OrderDemand order = OrderDemand.builder()
                .id(30L)
                .warehouse(warehouse)
                .orderNumber("O-1")
                .orderDateTime(LocalDateTime.of(2026, 6, 10, 10, 0))
                .build();
        List<OrderDemandItem> items = List.of(
                item(40L, warehouse, order, popular, 12),
                item(50L, warehouse, order, slow, 3)
        );
        StoragePlace far = StoragePlace.builder().id(60L).distanceFromEntryMm(9_000).build();
        Container stored = Container.builder()
                .id(70L)
                .warehouse(warehouse)
                .article(popular)
                .quantity(40)
                .currentStoragePlace(far)
                .status(ContainerStatus.STORED)
                .build();

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(orderDemandItemRepository.findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
                any(),
                any(),
                any()
        )).thenReturn(items);
        when(containerRepository.findByWarehouseIdAndStatus(1L, ContainerStatus.STORED))
                .thenReturn(List.of(stored));
        when(scoringService.calculateDetailed(any(), any(), any())).thenReturn(Map.of(
                popular.getId(), new DemandForecastScore(
                        new ArticleDemandScore(popular.getId(), 25.0, 12, 1),
                        DemandScoreSource.TRIBUO,
                        "DFM-1",
                        14
                ),
                slow.getId(), new DemandForecastScore(
                        new ArticleDemandScore(slow.getId(), 2.0, 3, 1),
                        DemandScoreSource.BASELINE,
                        "DFM-1",
                        14
                )
        ));

        var result = service.getArticles(
                1L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 10)
        );

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().articleNumber()).isEqualTo("100");
        assertThat(result.getFirst().rank()).isEqualTo(1);
        assertThat(result.getFirst().scoreSource()).isEqualTo(DemandScoreSource.TRIBUO);
        assertThat(result.getFirst().storedQuantity()).isEqualTo(40);
        assertThat(result.getFirst().averageDistanceFromEntryMm()).isEqualByComparingTo("9000.00");
        assertThat(result.getFirst().explanation()).contains("Tribuo", "9000.00 mm");
    }

    @Test
    void rejectsUnboundedTopLimit() {
        assertThatThrownBy(() -> service.getTopArticles(1L, 101, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 100");
    }

    private OrderDemandItem item(
            Long id,
            Warehouse warehouse,
            OrderDemand order,
            Article article,
            int quantity
    ) {
        return OrderDemandItem.builder()
                .id(id)
                .warehouse(warehouse)
                .orderDemand(order)
                .article(article)
                .quantity(quantity)
                .build();
    }
}
