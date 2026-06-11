package com.nikitaopara.warehouseoptimizer.search.dto;

import com.nikitaopara.warehouseoptimizer.search.model.WarehouseAuditEventDocument;

import java.time.Instant;
import java.util.Map;

public record WarehouseAuditEventResponse(
        String eventId,
        String topic,
        String eventType,
        String aggregateType,
        String aggregateId,
        String warehouseKey,
        Instant occurredAt,
        Map<String, Object> payload
) {
    public static WarehouseAuditEventResponse from(WarehouseAuditEventDocument document) {
        return new WarehouseAuditEventResponse(
                document.id(),
                document.topic(),
                document.eventType(),
                document.aggregateType(),
                document.aggregateId(),
                document.warehouseKey(),
                document.occurredAt(),
                document.payload()
        );
    }
}
