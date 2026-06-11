package com.nikitaopara.warehouseoptimizer.search.model;

import com.nikitaopara.warehouseoptimizer.eventing.model.DomainEventEnvelope;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Map;

@Document(indexName = "warehouse-audit-events", createIndex = false)
public record WarehouseAuditEventDocument(
        @Id String id,
        @Field(type = FieldType.Keyword) String topic,
        @Field(type = FieldType.Keyword) String eventType,
        @Field(type = FieldType.Keyword) String aggregateType,
        @Field(type = FieldType.Keyword) String aggregateId,
        @Field(type = FieldType.Keyword) String warehouseKey,
        @Field(type = FieldType.Date, format = DateFormat.date_time) Instant occurredAt,
        @Field(type = FieldType.Object) Map<String, Object> payload
) {
    public static WarehouseAuditEventDocument from(
            String topic,
            String warehouseKey,
            DomainEventEnvelope envelope,
            Map<String, Object> payload
    ) {
        return new WarehouseAuditEventDocument(
                envelope.eventId().toString(),
                topic,
                envelope.eventType(),
                envelope.aggregateType(),
                envelope.aggregateId(),
                warehouseKey,
                envelope.occurredAt(),
                payload
        );
    }
}
