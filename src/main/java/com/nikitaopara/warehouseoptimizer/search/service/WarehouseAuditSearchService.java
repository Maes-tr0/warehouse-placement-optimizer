package com.nikitaopara.warehouseoptimizer.search.service;

import com.nikitaopara.warehouseoptimizer.search.config.SearchProperties;
import com.nikitaopara.warehouseoptimizer.search.dto.WarehouseAuditEventResponse;
import com.nikitaopara.warehouseoptimizer.search.dto.WarehouseAuditSearchResponse;
import com.nikitaopara.warehouseoptimizer.search.model.WarehouseAuditEventDocument;
import com.nikitaopara.warehouseoptimizer.search.repository.WarehouseAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseAuditSearchService {

    private final WarehouseAuditEventRepository repository;
    private final SearchProperties properties;

    public WarehouseAuditSearchResponse search(
            String warehouseCode,
            String eventType,
            int page,
            int size
    ) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Elasticsearch audit search is disabled");
        }
        if (warehouseCode == null || warehouseCode.isBlank()) {
            throw new IllegalArgumentException("warehouseCode is required");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page cannot be negative");
        }
        if (size < 1 || size > properties.getMaximumPageSize()) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + properties.getMaximumPageSize()
            );
        }

        var pageable = PageRequest.of(page, size);
        Page<WarehouseAuditEventDocument> result =
                eventType == null || eventType.isBlank()
                        ? repository.findByWarehouseKeyOrderByOccurredAtDesc(
                        warehouseCode,
                        pageable
                )
                        : repository.findByWarehouseKeyAndEventTypeOrderByOccurredAtDesc(
                        warehouseCode,
                        eventType,
                        pageable
                );

        return new WarehouseAuditSearchResponse(
                result.getContent().stream().map(WarehouseAuditEventResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
