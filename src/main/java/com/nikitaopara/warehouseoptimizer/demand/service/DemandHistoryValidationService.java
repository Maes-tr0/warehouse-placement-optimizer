package com.nikitaopara.warehouseoptimizer.demand.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandHistoryRequest;
import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandOrderItemRequest;
import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandOrderRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
public class DemandHistoryValidationService {

    public void validateImportDemandHistoryRequest(
            User actor,
            ImportDemandHistoryRequest request
    ) {
        validateAdminActor(actor);

        if (request == null) {
            throw new IllegalArgumentException("Import demand history request cannot be null");
        }

        if (request.warehouseId() == null) {
            throw new IllegalArgumentException("Warehouse id is required");
        }

        if (request.orders() == null || request.orders().isEmpty()) {
            throw new IllegalArgumentException("Orders are required");
        }

        validateOrders(request);
    }

    private void validateOrders(ImportDemandHistoryRequest request) {
        Set<String> orderNumbers = new HashSet<>();

        for (ImportDemandOrderRequest order : request.orders()) {
            validateOrder(order);

            String normalizedOrderNumber = order.orderNumber().trim();

            if (!orderNumbers.add(normalizedOrderNumber)) {
                throw new IllegalArgumentException(
                        "Duplicate order number in import request: " + normalizedOrderNumber
                );
            }
        }
    }

    private void validateOrder(ImportDemandOrderRequest order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (!StringUtils.hasText(order.orderNumber())) {
            throw new IllegalArgumentException("Order number is required");
        }

        if (order.orderDateTime() == null) {
            throw new IllegalArgumentException(
                    "Order date time is required. Order: " + order.orderNumber()
            );
        }

        if (order.items() == null || order.items().isEmpty()) {
            throw new IllegalArgumentException(
                    "Order items are required. Order: " + order.orderNumber()
            );
        }

        for (ImportDemandOrderItemRequest item : order.items()) {
            validateOrderItem(item, order.orderNumber());
        }
    }

    private void validateOrderItem(
            ImportDemandOrderItemRequest item,
            String orderNumber
    ) {
        if (item == null) {
            throw new IllegalArgumentException(
                    "Order item cannot be null. Order: " + orderNumber
            );
        }

        if (!StringUtils.hasText(item.articleNumber())) {
            throw new IllegalArgumentException(
                    "Article number is required. Order: " + orderNumber
            );
        }

        if (item.quantity() == null || item.quantity() <= 0) {
            throw new IllegalArgumentException(
                    "Item quantity must be greater than zero. Order: " + orderNumber
            );
        }
    }

    private void validateAdminActor(User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() != Role.ROOT_ADMIN && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN or ROOT_ADMIN can import demand history");
        }
    }
}