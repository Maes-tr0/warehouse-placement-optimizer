package com.nikitaopara.warehouseoptimizer.demand.controller;

import com.nikitaopara.warehouseoptimizer.demand.dto.DemandArticleAnalyticsResponse;
import com.nikitaopara.warehouseoptimizer.demand.service.DemandAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/warehouses/{warehouseId}/demand/articles")
@RequiredArgsConstructor
public class DemandAnalyticsController {

    private final DemandAnalyticsService demandAnalyticsService;

    @GetMapping
    public List<DemandArticleAnalyticsResponse> getArticles(
            @PathVariable Long warehouseId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return demandAnalyticsService.getArticles(warehouseId, from, to);
    }

    @GetMapping("/top")
    public List<DemandArticleAnalyticsResponse> getTopArticles(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return demandAnalyticsService.getTopArticles(warehouseId, limit, from, to);
    }

    @GetMapping("/{articleNumber}")
    public DemandArticleAnalyticsResponse getArticle(
            @PathVariable Long warehouseId,
            @PathVariable String articleNumber,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return demandAnalyticsService.getArticle(warehouseId, articleNumber, from, to);
    }
}
