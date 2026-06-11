package com.nikitaopara.warehouseoptimizer.demand.service;

import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.cache.config.CacheNames;
import com.nikitaopara.warehouseoptimizer.demand.dto.DemandArticleAnalyticsResponse;
import com.nikitaopara.warehouseoptimizer.demand.forecast.model.DemandForecastScore;
import com.nikitaopara.warehouseoptimizer.demand.forecast.service.DemandForecastScoringService;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemandItem;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.model.DemandObservation;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DemandAnalyticsService {

    private final WarehouseRepository warehouseRepository;
    private final OrderDemandItemRepository orderDemandItemRepository;
    private final ContainerRepository containerRepository;
    private final DemandForecastScoringService scoringService;
    private final OptimizationProperties optimizationProperties;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.DEMAND_ANALYTICS,
            key = "'articles:' + #warehouseId + ':' + #from + ':' + #to"
    )
    public List<DemandArticleAnalyticsResponse> getArticles(
            Long warehouseId,
            LocalDate from,
            LocalDate to
    ) {
        validateWarehouse(warehouseId);
        DateRange range = resolveRange(from, to);
        List<OrderDemandItem> items = orderDemandItemRepository
                .findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
                        warehouseId,
                        range.from(),
                        range.to()
                );

        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, Article> articles = new LinkedHashMap<>();
        List<DemandObservation> observations = items.stream()
                .map(item -> {
                    articles.putIfAbsent(item.getArticle().getId(), item.getArticle());
                    return new DemandObservation(
                            item.getArticle().getId(),
                            item.getOrderDemand().getId(),
                            item.getOrderDemand().getOrderDateTime(),
                            item.getQuantity()
                    );
                })
                .toList();
        Map<Long, DemandForecastScore> scores = scoringService.calculateDetailed(
                warehouseId,
                observations,
                range.to().toLocalDate()
        );
        Map<Long, InventorySummary> inventory = summarizeInventory(
                containerRepository.findByWarehouseIdAndStatus(warehouseId, ContainerStatus.STORED)
        );

        List<UnrankedAnalytics> unranked = new ArrayList<>();
        articles.forEach((articleId, article) -> {
            DemandForecastScore forecast = scores.get(articleId);

            if (forecast != null) {
                unranked.add(new UnrankedAnalytics(
                        article,
                        forecast,
                        inventory.getOrDefault(articleId, InventorySummary.EMPTY)
                ));
            }
        });
        unranked.sort(Comparator
                .comparingDouble((UnrankedAnalytics row) -> row.forecast().score().weightedDemand())
                .reversed()
                .thenComparing(row -> row.article().getArticleNumber()));

        List<DemandArticleAnalyticsResponse> result = new ArrayList<>(unranked.size());

        for (int index = 0; index < unranked.size(); index++) {
            result.add(toResponse(index + 1, unranked.get(index)));
        }

        return List.copyOf(result);
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.DEMAND_ANALYTICS,
            key = "'top:' + #warehouseId + ':' + #limit + ':' + #from + ':' + #to"
    )
    public List<DemandArticleAnalyticsResponse> getTopArticles(
            Long warehouseId,
            int limit,
            LocalDate from,
            LocalDate to
    ) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Demand analytics limit must be between 1 and 100");
        }

        return getArticles(warehouseId, from, to).stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.DEMAND_ANALYTICS,
            key = "'article:' + #warehouseId + ':' + #articleNumber + ':' + #from + ':' + #to"
    )
    public DemandArticleAnalyticsResponse getArticle(
            Long warehouseId,
            String articleNumber,
            LocalDate from,
            LocalDate to
    ) {
        if (articleNumber == null || articleNumber.isBlank()) {
            throw new IllegalArgumentException("Article number is required");
        }

        return getArticles(warehouseId, from, to).stream()
                .filter(response -> response.articleNumber().equals(articleNumber.trim()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demand analytics not found for article: " + articleNumber
                ));
    }

    private Map<Long, InventorySummary> summarizeInventory(List<Container> containers) {
        Map<Long, MutableInventorySummary> mutable = new HashMap<>();

        for (Container container : containers) {
            if (container.getArticle() == null
                    || container.getQuantity() == null
                    || container.getQuantity() <= 0
                    || container.getCurrentStoragePlace() == null
                    || container.getCurrentStoragePlace().getDistanceFromEntryMm() == null) {
                continue;
            }

            mutable.computeIfAbsent(
                    container.getArticle().getId(),
                    ignored -> new MutableInventorySummary()
            ).add(container);
        }

        Map<Long, InventorySummary> result = new HashMap<>();
        mutable.forEach((articleId, summary) -> result.put(articleId, summary.toImmutable()));
        return result;
    }

    private DemandArticleAnalyticsResponse toResponse(int rank, UnrankedAnalytics row) {
        var demand = row.forecast().score();
        InventorySummary inventory = row.inventory();

        return new DemandArticleAnalyticsResponse(
                rank,
                row.article().getId(),
                row.article().getArticleNumber(),
                row.article().getName(),
                demand.weightedDemand(),
                row.forecast().source(),
                row.forecast().modelCode(),
                row.forecast().forecastHorizonDays(),
                demand.totalQuantity(),
                demand.orderCount(),
                inventory.quantity(),
                inventory.containerCount(),
                inventory.averageDistanceMm(),
                explain(rank, row.forecast(), inventory)
        );
    }

    private String explain(
            int rank,
            DemandForecastScore forecast,
            InventorySummary inventory
    ) {
        String demandExplanation = switch (forecast.source()) {
            case TRIBUO -> "Demand predicted by active Tribuo model";
            case BASELINE -> "Cold-start demand estimated from the latest 28 days";
            case SEASONAL -> "Demand ranked by recency and seasonality because no usable model is active";
        };

        if (inventory.containerCount() == 0) {
            return demandExplanation + "; no stored inventory is currently available";
        }

        return demandExplanation
                + "; demand rank is " + rank
                + " and quantity-weighted average distance is "
                + inventory.averageDistanceMm() + " mm";
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        LocalDate resolvedFrom = from == null
                ? resolvedTo.minusDays(optimizationProperties.getLookbackDays())
                : from;

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("Demand analytics start date cannot be after end date");
        }

        return new DateRange(resolvedFrom.atStartOfDay(), resolvedTo.atTime(LocalTime.MAX));
    }

    private void validateWarehouse(Long warehouseId) {
        if (warehouseId == null || !warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found: " + warehouseId);
        }
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {
    }

    private record UnrankedAnalytics(
            Article article,
            DemandForecastScore forecast,
            InventorySummary inventory
    ) {
    }

    private record InventorySummary(
            long quantity,
            long containerCount,
            BigDecimal averageDistanceMm
    ) {
        private static final InventorySummary EMPTY = new InventorySummary(0, 0, null);
    }

    private static final class MutableInventorySummary {
        private long quantity;
        private long containerCount;
        private long quantityWeightedDistance;

        private void add(Container container) {
            quantity += container.getQuantity();
            containerCount++;
            quantityWeightedDistance += (long) container.getQuantity()
                    * container.getCurrentStoragePlace().getDistanceFromEntryMm();
        }

        private InventorySummary toImmutable() {
            BigDecimal average = quantity == 0
                    ? null
                    : BigDecimal.valueOf(quantityWeightedDistance)
                            .divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
            return new InventorySummary(quantity, containerCount, average);
        }
    }
}
