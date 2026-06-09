package com.nikitaopara.warehouseoptimizer.putaway.placement.controller;

import com.nikitaopara.warehouseoptimizer.putaway.placement.dto.ApprovePlacementRecommendationRequest;
import com.nikitaopara.warehouseoptimizer.putaway.placement.dto.PlacementRecommendationResponse;
import com.nikitaopara.warehouseoptimizer.putaway.placement.dto.RecommendPlacementRequest;
import com.nikitaopara.warehouseoptimizer.putaway.placement.service.PlacementRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operator/placement")
@RequiredArgsConstructor
public class PlacementRecommendationController {

    private final PlacementRecommendationService placementRecommendationService;

    @PostMapping("/recommend")
    public ResponseEntity<PlacementRecommendationResponse> recommendPlacement(
            @RequestBody RecommendPlacementRequest request
    ) {
        PlacementRecommendationResponse response =
                placementRecommendationService.recommendPlacement(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recommendations/{recommendationCode}/approve")
    public ResponseEntity<PlacementRecommendationResponse> approveRecommendation(
            @PathVariable String recommendationCode,
            @RequestBody ApprovePlacementRecommendationRequest request
    ) {
        PlacementRecommendationResponse response =
                placementRecommendationService.approveRecommendation(recommendationCode, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recommendations/{recommendationCode}/reject")
    public ResponseEntity<PlacementRecommendationResponse> rejectRecommendation(
            @PathVariable String recommendationCode
    ) {
        PlacementRecommendationResponse response =
                placementRecommendationService.rejectRecommendation(recommendationCode);

        return ResponseEntity.ok(response);
    }
}