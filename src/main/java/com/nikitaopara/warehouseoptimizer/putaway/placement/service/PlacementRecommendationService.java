package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.MergeContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.PlaceContainerRequest;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDataService;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerService;
import com.nikitaopara.warehouseoptimizer.putaway.placement.dto.ApprovePlacementRecommendationRequest;
import com.nikitaopara.warehouseoptimizer.putaway.placement.dto.PlacementRecommendationResponse;
import com.nikitaopara.warehouseoptimizer.putaway.placement.dto.RecommendPlacementRequest;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementRecommendation;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementScoreResult;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlacementRecommendationService {

    private final AuthenticatedUserService authenticatedUserService;
    private final ContainerDataService containerDataService;
    private final ContainerService containerService;

    private final PlacementRecommendationValidationService validationService;
    private final PlacementRecommendationDataService dataService;
    private final PlacementCandidateService candidateService;
    private final PlacementScoringService scoringService;
    private final PlacementRecommendationFactory recommendationFactory;

    @Transactional
    public PlacementRecommendationResponse recommendPlacement(RecommendPlacementRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        Container sourceContainer = getValidatedSourceContainer(actor, request);

        dataService.expireSuggestedRecommendations();

        return dataService.findSuggestedRecommendationBySourceContainer(sourceContainer)
                .map(this::reuseSuggestedRecommendation)
                .orElseGet(() -> createAndSaveNewRecommendation(sourceContainer));
    }

    @Transactional
    public PlacementRecommendationResponse approveRecommendation(
            String recommendationCode,
            ApprovePlacementRecommendationRequest request
    ) {
        User actor = authenticatedUserService.getCurrentUser();

        validationService.validateApproveRecommendationRequest(actor, recommendationCode, request);

        dataService.expireSuggestedRecommendations();

        PlacementRecommendation recommendation = dataService.getByCodeOrThrow(recommendationCode);

        validationService.validateRecommendationCanBeAccepted(recommendation);
        validationService.validateScannedStoragePlaceMatchesRecommendation(recommendation, request);

        executeRecommendation(recommendation);

        PlacementRecommendation acceptedRecommendation = dataService.accept(recommendation);

        return PlacementRecommendationResponse.from(acceptedRecommendation);
    }

    @Transactional
    public PlacementRecommendationResponse rejectRecommendation(String recommendationCode) {
        User actor = authenticatedUserService.getCurrentUser();

        validationService.validateRejectRecommendationRequest(actor, recommendationCode);

        dataService.expireSuggestedRecommendations();

        PlacementRecommendation recommendation = dataService.getByCodeOrThrow(recommendationCode);

        validationService.validateRecommendationCanBeRejected(recommendation);

        PlacementRecommendation rejectedRecommendation = dataService.reject(recommendation);

        return PlacementRecommendationResponse.from(rejectedRecommendation);
    }

    private Container getValidatedSourceContainer(
            User actor,
            RecommendPlacementRequest request
    ) {
        validationService.validateRecommendPlacementRequest(actor, request);

        Container sourceContainer = containerDataService.getByContainerNumberOrThrow(
                request.containerNumber().trim()
        );

        validationService.validateSourceContainerForRecommendation(sourceContainer);

        return sourceContainer;
    }

    private PlacementRecommendationResponse reuseSuggestedRecommendation(
            PlacementRecommendation existingSuggestedRecommendation
    ) {
        validationService.validateSuggestedRecommendationForReuse(existingSuggestedRecommendation);

        return PlacementRecommendationResponse.from(existingSuggestedRecommendation);
    }

    private PlacementRecommendationResponse createAndSaveNewRecommendation(
            Container sourceContainer
    ) {
        PlacementRecommendation recommendation = createBestRecommendation(sourceContainer);

        PlacementRecommendation savedRecommendation = dataService.save(recommendation);

        return PlacementRecommendationResponse.from(savedRecommendation);
    }

    private PlacementRecommendation createBestRecommendation(Container sourceContainer) {
        return candidateService.findBestMergeRecommendationCandidate(sourceContainer)
                .map(targetContainer -> createMergeRecommendation(sourceContainer, targetContainer))
                .orElseGet(() -> createPlaceRecommendation(sourceContainer));
    }

    private PlacementRecommendation createMergeRecommendation(
            Container sourceContainer,
            Container targetContainer
    ) {
        PlacementScoreResult scoreResult = scoringService.calculateMergeScore(
                sourceContainer,
                targetContainer
        );

        return recommendationFactory.createMergeRecommendation(
                sourceContainer,
                targetContainer,
                scoreResult
        );
    }

    private PlacementRecommendation createPlaceRecommendation(Container sourceContainer) {
        StoragePlace storagePlace = candidateService
                .findBestPlaceRecommendationCandidate(sourceContainer)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No available placement recommendation found"
                ));

        PlacementScoreResult scoreResult = scoringService.calculatePlaceScore(
                sourceContainer,
                storagePlace
        );

        return recommendationFactory.createPlaceRecommendation(
                sourceContainer,
                storagePlace,
                scoreResult
        );
    }

    private void executeRecommendation(PlacementRecommendation recommendation) {
        if (recommendation.isMergeRecommendation()) {
            executeMergeRecommendation(recommendation);
            return;
        }

        if (recommendation.isPlaceRecommendation()) {
            executePlaceRecommendation(recommendation);
            return;
        }

        throw new IllegalArgumentException("Unsupported placement recommendation type");
    }

    private void executeMergeRecommendation(PlacementRecommendation recommendation) {
        String sourceContainerNumber = recommendation
                .getSourceContainer()
                .getContainerNumber();

        String targetContainerNumber = recommendation
                .getTargetContainer()
                .getContainerNumber();

        containerService.mergeContainer(
                sourceContainerNumber,
                new MergeContainerRequest(targetContainerNumber)
        );
    }

    private void executePlaceRecommendation(PlacementRecommendation recommendation) {
        String sourceContainerNumber = recommendation
                .getSourceContainer()
                .getContainerNumber();

        String storagePlaceCode = recommendation
                .getRecommendedStoragePlace()
                .getCode();

        containerService.placeContainer(
                sourceContainerNumber,
                new PlaceContainerRequest(storagePlaceCode)
        );
    }
}
