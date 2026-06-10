package com.nikitaopara.warehouseoptimizer.demand.controller;

import com.nikitaopara.warehouseoptimizer.demand.dto.DemandHistoryImportResponse;
import com.nikitaopara.warehouseoptimizer.demand.dto.ImportDemandHistoryRequest;
import com.nikitaopara.warehouseoptimizer.demand.service.DemandHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/demand-history")
@RequiredArgsConstructor
public class DemandHistoryController {

    private final DemandHistoryService demandHistoryService;

    @PostMapping("/import")
    public ResponseEntity<DemandHistoryImportResponse> importDemandHistory(
            @RequestBody ImportDemandHistoryRequest request
    ) {
        DemandHistoryImportResponse response = demandHistoryService.importDemandHistory(request);

        return ResponseEntity.ok(response);
    }
}