package com.nikitaopara.warehouseoptimizer.search.controller;

import com.nikitaopara.warehouseoptimizer.search.dto.WarehouseAuditSearchResponse;
import com.nikitaopara.warehouseoptimizer.search.service.WarehouseAuditSearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Events", description = "Searchable warehouse audit events from Elasticsearch")
@RestController
@RequestMapping("/admin/audit/events")
@RequiredArgsConstructor
public class WarehouseAuditSearchController {

    private final WarehouseAuditSearchService searchService;

    @GetMapping
    public WarehouseAuditSearchResponse search(
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return searchService.search(warehouseCode, eventType, page, size);
    }
}
