package com.nikitaopara.warehouseoptimizer.demand.service;

import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemand;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandRepository;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.article.repository.ArticleRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandHistoryDataService {

    private final WarehouseRepository warehouseRepository;
    private final ArticleRepository articleRepository;
    private final OrderDemandRepository orderDemandRepository;

    public Warehouse getWarehouseByIdOrThrow(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse not found by id: " + warehouseId
                ));
    }

    public Map<String, Article> getArticlesByNumbersOrThrow(Set<String> articleNumbers) {
        if (articleNumbers == null || articleNumbers.isEmpty()) {
            return Map.of();
        }

        List<Article> articles = articleRepository.findByArticleNumberIn(articleNumbers);

        Map<String, Article> articlesByNumber = articles.stream()
                .collect(Collectors.toMap(
                        Article::getArticleNumber,
                        Function.identity()
                ));

        Set<String> missingArticleNumbers = new TreeSet<>(articleNumbers);
        missingArticleNumbers.removeAll(articlesByNumber.keySet());

        if (!missingArticleNumbers.isEmpty()) {
            throw new IllegalArgumentException(
                    "Articles not found: " + missingArticleNumbers
            );
        }

        return articlesByNumber;
    }

    public Set<String> getExistingOrderNumbers(
            Long warehouseId,
            Set<String> orderNumbers
    ) {
        if (warehouseId == null || orderNumbers == null || orderNumbers.isEmpty()) {
            return Set.of();
        }

        return orderDemandRepository
                .findByWarehouseIdAndOrderNumberIn(warehouseId, orderNumbers)
                .stream()
                .map(OrderDemand::getOrderNumber)
                .collect(Collectors.toSet());
    }

    public List<OrderDemand> saveAll(List<OrderDemand> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        return orderDemandRepository.saveAll(orders);
    }
}
