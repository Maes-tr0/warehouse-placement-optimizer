package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.placement.dto.ApprovePlacementRecommendationRequest;
import com.nikitaopara.warehouseoptimizer.putaway.placement.dto.RecommendPlacementRequest;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendation;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PlacementRecommendationValidationService {

    public void validateRecommendPlacementRequest(User actor, RecommendPlacementRequest request) {
        validateOperatorOrAdmin(actor);

        if (request == null) {
            throw new IllegalArgumentException("Recommend placement request cannot be null");
        }

        if (!StringUtils.hasText(request.containerNumber())) {
            throw new IllegalArgumentException("Container number is required");
        }
    }

    public void validateApproveRecommendationRequest(
            User actor,
            String recommendationCode,
            ApprovePlacementRecommendationRequest request
    ) {
        validateOperatorOrAdmin(actor);

        if (!StringUtils.hasText(recommendationCode)) {
            throw new IllegalArgumentException("Recommendation code is required");
        }

        if (request == null) {
            throw new IllegalArgumentException("Approve recommendation request cannot be null");
        }

        if (!StringUtils.hasText(request.scannedStoragePlaceCode())) {
            throw new IllegalArgumentException("Scanned storage place code is required");
        }
    }

    public void validateRejectRecommendationRequest(
            User actor,
            String recommendationCode
    ) {
        validateOperatorOrAdmin(actor);

        if (!StringUtils.hasText(recommendationCode)) {
            throw new IllegalArgumentException("Recommendation code is required");
        }
    }

    public void validateScannedStoragePlaceMatchesRecommendation(
            PlacementRecommendation recommendation,
            ApprovePlacementRecommendationRequest request
    ) {
        if (recommendation == null) {
            throw new IllegalArgumentException("Placement recommendation is required");
        }

        if (recommendation.getRecommendedStoragePlace() == null) {
            throw new IllegalArgumentException("Placement recommendation must have recommended storage place");
        }

        String expectedStoragePlaceCode = recommendation.getRecommendedStoragePlace().getCode();
        String scannedStoragePlaceCode = request.scannedStoragePlaceCode().trim();

        if (!expectedStoragePlaceCode.equalsIgnoreCase(scannedStoragePlaceCode)) {
            throw new IllegalArgumentException(
                    "Scanned storage place does not match recommendation. Expected: "
                            + expectedStoragePlaceCode +
                            ", scanned: " +
                            scannedStoragePlaceCode
            );
        }
    }

    public void validateSourceContainerForRecommendation(Container sourceContainer) {
        if (sourceContainer == null) {
            throw new IllegalArgumentException("Source container is required");
        }

        if (sourceContainer.getWarehouse() == null) {
            throw new IllegalArgumentException("Source container must belong to warehouse");
        }

        if (sourceContainer.getArticle() == null) {
            throw new IllegalArgumentException("Source container must have article");
        }

        if (!sourceContainer.isWaitingForPlacement()) {
            throw new IllegalArgumentException("Only container waiting for placement can be recommended");
        }

        if (sourceContainer.getQuantity() == null || sourceContainer.getQuantity() <= 0) {
            throw new IllegalArgumentException("Source container quantity must be greater than zero");
        }

        if (sourceContainer.getWeightKg() == null || sourceContainer.getWeightKg().signum() <= 0) {
            throw new IllegalArgumentException("Source container weight must be greater than zero");
        }

        if (sourceContainer.getHeightMm() == null || sourceContainer.getHeightMm() <= 0) {
            throw new IllegalArgumentException("Source container height must be greater than zero");
        }
    }

    public void validateSuggestedRecommendationForReuse(PlacementRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("Placement recommendation is required");
        }

        if (!recommendation.isSuggested()) {
            throw new IllegalArgumentException("Only suggested recommendation can be reused");
        }

        if (recommendation.getSourceContainer() == null) {
            throw new IllegalArgumentException("Placement recommendation must have source container");
        }

        if (!recommendation.getSourceContainer().isWaitingForPlacement()) {
            throw new IllegalArgumentException("Recommendation source container is no longer waiting for placement");
        }
    }

    public void validateRecommendationCanBeRejected(PlacementRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("Placement recommendation is required");
        }

        if (!recommendation.isSuggested()) {
            throw new IllegalArgumentException("Only suggested recommendation can be rejected");
        }
    }

    public void validateRecommendationCanBeAccepted(PlacementRecommendation recommendation) {
        if (recommendation == null) {
            throw new IllegalArgumentException("Placement recommendation is required");
        }

        if (!recommendation.isSuggested()) {
            throw new IllegalArgumentException("Only suggested recommendation can be accepted");
        }

        if (recommendation.getSourceContainer() == null) {
            throw new IllegalArgumentException("Placement recommendation must have source container");
        }

        if (!recommendation.getSourceContainer().isWaitingForPlacement()) {
            throw new IllegalArgumentException("Recommendation source container is no longer waiting for placement");
        }

        if (recommendation.isMergeRecommendation()) {
            validateMergeRecommendationCanBeAccepted(recommendation);
            return;
        }

        if (recommendation.isPlaceRecommendation()) {
            validatePlaceRecommendationCanBeAccepted(recommendation);
            return;
        }

        throw new IllegalArgumentException("Unsupported placement recommendation type");
    }

    private void validateMergeRecommendationCanBeAccepted(PlacementRecommendation recommendation) {
        if (recommendation.getTargetContainer() == null) {
            throw new IllegalArgumentException("Merge recommendation must have target container");
        }

        if (!recommendation.getTargetContainer().isStored()) {
            throw new IllegalArgumentException("Merge target container must be stored");
        }

        if (recommendation.getRecommendedStoragePlace() == null) {
            throw new IllegalArgumentException("Merge recommendation must have target storage place");
        }

        Container sourceContainer = recommendation.getSourceContainer();
        Container targetContainer = recommendation.getTargetContainer();

        if (sourceContainer.getWarehouse() == null || targetContainer.getWarehouse() == null) {
            throw new IllegalArgumentException("Both containers must belong to warehouse");
        }

        if (!sourceContainer.getWarehouse().getId().equals(targetContainer.getWarehouse().getId())) {
            throw new IllegalArgumentException("Containers must belong to the same warehouse");
        }

        if (sourceContainer.getArticle() == null || targetContainer.getArticle() == null) {
            throw new IllegalArgumentException("Both containers must have article");
        }

        if (!sourceContainer.getArticle().getId().equals(targetContainer.getArticle().getId())) {
            throw new IllegalArgumentException("Only containers with the same article can be merged");
        }

        if (!targetContainer.canAcceptQuantity(sourceContainer.getQuantity())) {
            throw new IllegalArgumentException("Target container does not have enough quantity capacity");
        }
    }

    private void validatePlaceRecommendationCanBeAccepted(PlacementRecommendation recommendation) {
        if (recommendation.getRecommendedStoragePlace() == null) {
            throw new IllegalArgumentException("Place recommendation must have recommended storage place");
        }
    }

    private void validateOperatorOrAdmin(User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() != Role.ROOT_ADMIN
                && actor.getRole() != Role.ADMIN
                && actor.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("Only OPERATOR, ADMIN or ROOT_ADMIN can request placement recommendation");
        }
    }
}