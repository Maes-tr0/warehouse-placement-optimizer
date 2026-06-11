package com.nikitaopara.warehouseoptimizer.demand.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.cache.config.CacheNames;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.demand.dto.DemandHistoryImportResponse;
import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandHistoryRequest;
import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandOrderItemRequest;
import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandOrderRequest;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemand;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandHistoryService {

    private final AuthenticatedUserService authenticatedUserService;

    private final DemandHistoryValidationService validationService;
    private final DemandHistoryDataService dataService;
    private final DemandHistoryImportMapper mapperService;

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DEMAND_ANALYTICS, allEntries = true)
    public DemandHistoryImportResponse importDemandHistory(ImportDemandHistoryRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        validationService.validateImportDemandHistoryRequest(actor, request);

        Warehouse warehouse = dataService.getWarehouseByIdOrThrow(request.warehouseId());

        Set<String> orderNumbers = extractOrderNumbers(request);
        Set<String> articleNumbers = extractArticleNumbers(request);

        Map<String, Article> articlesByNumber =
                dataService.getArticlesByNumbersOrThrow(articleNumbers);

        Set<String> existingOrderNumbers =
                dataService.getExistingOrderNumbers(warehouse.getId(), orderNumbers);

        List<OrderDemand> ordersToSave = buildNewOrders(
                warehouse,
                request.orders(),
                articlesByNumber,
                existingOrderNumbers
        );

        List<OrderDemand> savedOrders = dataService.saveAll(ordersToSave);

        return new DemandHistoryImportResponse(
                warehouse.getId(),
                request.orders().size(),
                savedOrders.size(),
                request.orders().size() - savedOrders.size(),
                countTotalItems(request),
                countImportedItems(savedOrders)
        );
    }

    private List<OrderDemand> buildNewOrders(
            Warehouse warehouse,
            List<ImportDemandOrderRequest> orderRequests,
            Map<String, Article> articlesByNumber,
            Set<String> existingOrderNumbers
    ) {
        List<OrderDemand> orders = new ArrayList<>();

        for (ImportDemandOrderRequest orderRequest : orderRequests) {
            String orderNumber = orderRequest.orderNumber().trim();

            if (existingOrderNumbers.contains(orderNumber)) {
                continue;
            }

            OrderDemand orderDemand = mapperService.toOrderDemand(
                    warehouse,
                    orderRequest,
                    articlesByNumber
            );

            orders.add(orderDemand);
        }

        return orders;
    }

    private Set<String> extractOrderNumbers(ImportDemandHistoryRequest request) {
        return request.orders()
                .stream()
                .map(ImportDemandOrderRequest::orderNumber)
                .map(String::trim)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<String> extractArticleNumbers(ImportDemandHistoryRequest request) {
        return request.orders()
                .stream()
                .flatMap(order -> order.items().stream())
                .map(ImportDemandOrderItemRequest::articleNumber)
                .map(String::trim)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private int countTotalItems(ImportDemandHistoryRequest request) {
        return request.orders()
                .stream()
                .mapToInt(order -> order.items().size())
                .sum();
    }

    private int countImportedItems(List<OrderDemand> savedOrders) {
        return savedOrders.stream()
                .mapToInt(order -> order.getItems() != null ? order.getItems().size() : 0)
                .sum();
    }
}
