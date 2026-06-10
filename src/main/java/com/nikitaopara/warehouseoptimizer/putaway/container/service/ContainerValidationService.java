package com.nikitaopara.warehouseoptimizer.putaway.container.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.*;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ContainerValidationService {

    private final ContainerDataService containerDataService;

    public void validateReceiveContainerRequest(User actor, ReceiveContainerRequest request) {
        validateOperatorOrAdmin(actor);
        validateReceiveContainerFields(request);
        validateContainerNumberDoesNotExist(request.containerNumber());
    }

    public void validateReceiveContainersBatchRequest(
            User actor,
            ReceiveContainersBatchRequest request
    ) {
        validateOperatorOrAdmin(actor);

        if (request == null) {
            throw new IllegalArgumentException("Receive containers batch request cannot be null");
        }

        if (request.containers() == null || request.containers().isEmpty()) {
            throw new IllegalArgumentException("Containers are required");
        }

        for (ReceiveContainerRequest containerRequest : request.containers()) {
            validateReceiveContainerFields(containerRequest);
        }
    }

    public void validateUpdateContainerRequest(User actor, UpdateContainerRequest request) {
        validateAdminActor(actor);

        if (request == null) {
            throw new IllegalArgumentException("Update container request cannot be null");
        }

        if (request.quantity() != null) {
            validatePositiveInteger(request.quantity(), "Quantity must be greater than zero");
        }

        if (request.weightKg() != null) {
            validatePositiveBigDecimal(request.weightKg(), "Weight must be greater than zero");
        }

        if (request.heightMm() != null) {
            validatePositiveInteger(request.heightMm(), "Height must be greater than zero");
        }
    }

    public void validatePlaceContainerRequest(User actor, PlaceContainerRequest request) {
        validateOperatorOrAdmin(actor);

        if (request == null) {
            throw new IllegalArgumentException("Place container request cannot be null");
        }

        if (!StringUtils.hasText(request.storagePlaceCode())) {
            throw new IllegalArgumentException("Storage place code is required");
        }
    }

    public void validateMergeContainerRequest(User actor, MergeContainerRequest request) {
        validateOperatorOrAdmin(actor);

        if (request == null) {
            throw new IllegalArgumentException("Merge container request cannot be null");
        }

        if (!StringUtils.hasText(request.targetContainerNumber())) {
            throw new IllegalArgumentException("Target container number is required");
        }
    }

    public void validateContainerCanBePlaced(Container container, StoragePlace storagePlace) {
        if (container == null) {
            throw new IllegalArgumentException("Container is required");
        }

        if (storagePlace == null) {
            throw new IllegalArgumentException("Storage place is required");
        }

        if (!container.isWaitingForPlacement()) {
            throw new IllegalArgumentException("Only container waiting for placement can be placed");
        }

        if (storagePlace.getStatus() != StoragePlaceStatus.AVAILABLE) {
            throw new IllegalArgumentException("Storage place is not available");
        }

        validateContainerFitsStoragePlace(
                storagePlace,
                container.getWeightKg(),
                container.getHeightMm()
        );
    }

    public void validateContainersCanBeMerged(Container source, Container target) {
        if (source == null) {
            throw new IllegalArgumentException("Source container is required");
        }

        if (target == null) {
            throw new IllegalArgumentException("Target container is required");
        }

        if (!source.isWaitingForPlacement()) {
            throw new IllegalArgumentException("Only waiting container can be merged into another container");
        }

        if (!target.isStored()) {
            throw new IllegalArgumentException("Target container must be stored");
        }

        if (source.getWarehouse() == null || target.getWarehouse() == null) {
            throw new IllegalArgumentException("Both containers must belong to a warehouse");
        }

        if (!source.getWarehouse().getId().equals(target.getWarehouse().getId())) {
            throw new IllegalArgumentException("Containers must belong to the same warehouse");
        }

        Article sourceArticle = source.getArticle();
        Article targetArticle = target.getArticle();

        if (sourceArticle == null || targetArticle == null) {
            throw new IllegalArgumentException("Both containers must have an article");
        }

        if (!sourceArticle.getId().equals(targetArticle.getId())) {
            throw new IllegalArgumentException("Only containers with the same article can be merged");
        }

        if (!target.canAcceptQuantity(source.getQuantity())) {
            throw new IllegalArgumentException("Target container does not have enough quantity capacity");
        }
    }

    public void validateContainerCanBeManuallyUpdated(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Container is required");
        }

        if (container.isMerged()) {
            throw new IllegalArgumentException("Merged container cannot be updated");
        }

        if (container.isRemoved()) {
            throw new IllegalArgumentException("Removed container cannot be updated");
        }
    }

    public void validateContainerQuantityFitsArticle(Article article, Integer quantity) {
        if (article == null) {
            throw new IllegalArgumentException("Article is required");
        }

        if (!article.canFitQuantity(quantity)) {
            throw new IllegalArgumentException(
                    "Container quantity exceeds max quantity per pallet for article "
                            + article.getArticleNumber()
            );
        }
    }

    public void validateContainerFitsCurrentStoragePlace(
            Container container,
            BigDecimal weightKg,
            Integer heightMm
    ) {
        if (container == null) {
            throw new IllegalArgumentException("Container is required");
        }

        StoragePlace currentStoragePlace = container.getCurrentStoragePlace();

        if (currentStoragePlace == null) {
            return;
        }

        validateContainerFitsStoragePlace(currentStoragePlace, weightKg, heightMm);
    }

    public void validateRemoveContainer(User actor, Container container) {
        validateAdminActor(actor);

        if (container == null) {
            throw new IllegalArgumentException("Container is required");
        }

        if (container.isMerged()) {
            throw new IllegalArgumentException("Merged container is already closed");
        }

        if (container.isRemoved()) {
            throw new IllegalArgumentException("Container is already removed");
        }
    }

    private void validateReceiveContainerFields(ReceiveContainerRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Receive container request cannot be null");
        }

        if (request.warehouseId() == null) {
            throw new IllegalArgumentException("Warehouse id is required");
        }

        if (!StringUtils.hasText(request.containerNumber())) {
            throw new IllegalArgumentException("Container number is required");
        }

        if (!StringUtils.hasText(request.articleNumber())) {
            throw new IllegalArgumentException("Article number is required");
        }

        validatePositiveInteger(request.quantity(), "Quantity must be greater than zero");
        validatePositiveBigDecimal(request.weightKg(), "Weight must be greater than zero");
        validatePositiveInteger(request.heightMm(), "Height must be greater than zero");
    }

    private void validateContainerNumberDoesNotExist(String containerNumber) {
        String normalizedContainerNumber = containerNumber.trim();

        if (containerDataService.existsByContainerNumber(normalizedContainerNumber)) {
            throw new IllegalArgumentException(
                    "Container with this number already exists: " + normalizedContainerNumber
            );
        }
    }

    private void validateContainerFitsStoragePlace(
            StoragePlace storagePlace,
            BigDecimal weightKg,
            Integer heightMm
    ) {
        if (storagePlace == null) {
            throw new IllegalArgumentException("Storage place is required");
        }

        if (weightKg == null) {
            throw new IllegalArgumentException("Container weight is required");
        }

        if (heightMm == null) {
            throw new IllegalArgumentException("Container height is required");
        }

        if (weightKg.compareTo(BigDecimal.valueOf(storagePlace.getMaxWeightKg())) > 0) {
            throw new IllegalArgumentException("Container weight exceeds storage place max weight");
        }

        if (heightMm > storagePlace.getMaxHeightMm()) {
            throw new IllegalArgumentException("Container height exceeds storage place max height");
        }
    }

    private void validateOperatorOrAdmin(User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() != Role.ROOT_ADMIN
                && actor.getRole() != Role.ADMIN
                && actor.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("Only OPERATOR, ADMIN or ROOT_ADMIN can perform this action");
        }
    }

    private void validateAdminActor(User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() != Role.ROOT_ADMIN && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN or ROOT_ADMIN can update containers manually");
        }
    }

    private void validatePositiveInteger(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePositiveBigDecimal(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}