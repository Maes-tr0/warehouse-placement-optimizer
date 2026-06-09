package com.nikitaopara.warehouseoptimizer.demand.service;

import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandOrderItemRequest;
import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandOrderRequest;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemand;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemandItem;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DemandHistoryImportMapper {

    public OrderDemand toOrderDemand(
            Warehouse warehouse,
            ImportDemandOrderRequest orderRequest,
            Map<String, Article> articlesByNumber
    ) {
        OrderDemand orderDemand = OrderDemand.builder()
                .warehouse(warehouse)
                .orderNumber(orderRequest.orderNumber().trim())
                .orderDateTime(orderRequest.orderDateTime())
                .build();

        Map<String, Integer> quantityByArticleNumber = groupItemsByArticleNumber(orderRequest);

        for (Map.Entry<String, Integer> entry : quantityByArticleNumber.entrySet()) {
            String articleNumber = entry.getKey();
            Integer quantity = entry.getValue();

            Article article = articlesByNumber.get(articleNumber);

            OrderDemandItem item = OrderDemandItem.builder()
                    .article(article)
                    .quantity(quantity)
                    .build();

            orderDemand.addItem(item);
        }

        return orderDemand;
    }

    private Map<String, Integer> groupItemsByArticleNumber(
            ImportDemandOrderRequest orderRequest
    ) {
        Map<String, Integer> quantityByArticleNumber = new LinkedHashMap<>();

        for (ImportDemandOrderItemRequest item : orderRequest.items()) {
            String articleNumber = item.articleNumber().trim();

            quantityByArticleNumber.merge(
                    articleNumber,
                    item.quantity(),
                    Integer::sum
            );
        }

        return quantityByArticleNumber;
    }
}