package com.nikitaopara.warehouseoptimizer.search.repository;

import com.nikitaopara.warehouseoptimizer.search.model.WarehouseAuditEventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface WarehouseAuditEventRepository
        extends ElasticsearchRepository<WarehouseAuditEventDocument, String> {

    Page<WarehouseAuditEventDocument> findByWarehouseKeyOrderByOccurredAtDesc(
            String warehouseKey,
            Pageable pageable
    );

    Page<WarehouseAuditEventDocument> findByWarehouseKeyAndEventTypeOrderByOccurredAtDesc(
            String warehouseKey,
            String eventType,
            Pageable pageable
    );
}
