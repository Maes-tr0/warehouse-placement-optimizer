package com.nikitaopara.warehouseoptimizer.putaway.container.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovementType;
import com.nikitaopara.warehouseoptimizer.movement.service.ContainerMovementService;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.ArticleResponse;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.CreateArticlesBatchRequest;
import com.nikitaopara.warehouseoptimizer.putaway.article.dto.CreateArticlesBatchResponse;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.article.service.ArticleDataService;
import com.nikitaopara.warehouseoptimizer.putaway.container.dto.*;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContainerService {

    private final AuthenticatedUserService authenticatedUserService;
    private final ArticleDataService articleDataService;
    private final ContainerDataService containerDataService;
    private final ContainerValidationService containerValidationService;
    private final ContainerDimensionCalculationService dimensionCalculationService;
    private final ContainerMovementService movementService;

    @Transactional
    public ContainerResponse receiveContainer(ReceiveContainerRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        containerValidationService.validateReceiveContainerRequest(actor, request);

        Warehouse warehouse = containerDataService.getWarehouseByIdOrThrow(request.warehouseId());
        Article article = articleDataService.getByArticleNumberOrThrow(request.articleNumber().trim());

        containerValidationService.validateContainerQuantityFitsArticle(article, request.quantity());

        Container container = Container.builder()
                .warehouse(warehouse)
                .containerNumber(request.containerNumber().trim())
                .article(article)
                .quantity(request.quantity())
                .weightKg(request.weightKg())
                .heightMm(request.heightMm())
                .status(ContainerStatus.WAITING_FOR_PLACEMENT)
                .build();

        Container savedContainer = containerDataService.save(container);

        return ContainerResponse.from(savedContainer);
    }

    @Transactional(readOnly = true)
    public ContainerResponse getContainerByNumber(String containerNumber) {
        Container container = containerDataService.getByContainerNumberOrThrow(containerNumber);

        return ContainerResponse.from(container);
    }

    @Transactional(readOnly = true)
    public List<ContainerResponse> getContainers() {
        return containerDataService.getAll()
                .stream()
                .map(ContainerResponse::from)
                .toList();
    }

    @Transactional
    public ContainerResponse updateContainer(String containerNumber, UpdateContainerRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        containerValidationService.validateUpdateContainerRequest(actor, request);

        Container container = containerDataService.getByContainerNumberOrThrow(containerNumber);

        Integer newQuantity = request.quantity() != null
                ? request.quantity()
                : container.getQuantity();

        BigDecimal newWeightKg = request.weightKg() != null
                ? request.weightKg()
                : container.getWeightKg();

        Integer newHeightMm = request.heightMm() != null
                ? request.heightMm()
                : container.getHeightMm();

        containerValidationService.validateContainerCanBeManuallyUpdated(container);
        containerValidationService.validateContainerQuantityFitsArticle(container.getArticle(), newQuantity);
        containerValidationService.validateContainerFitsCurrentStoragePlace(container, newWeightKg, newHeightMm);

        container.setQuantity(newQuantity);
        container.setWeightKg(newWeightKg);
        container.setHeightMm(newHeightMm);

        Container savedContainer = containerDataService.save(container);

        return ContainerResponse.from(savedContainer);
    }

    @Transactional
    public ContainerResponse placeContainer(String containerNumber, PlaceContainerRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        containerValidationService.validatePlaceContainerRequest(actor, request);

        Container container = containerDataService.getByContainerNumberOrThrow(containerNumber);

        StoragePlace storagePlace = containerDataService.getStoragePlaceByWarehouseAndCodeOrThrow(
                container.getWarehouse().getId(),
                request.storagePlaceCode().trim()
        );

        containerValidationService.validateContainerCanBePlaced(container, storagePlace);

        storagePlace.setStatus(StoragePlaceStatus.OCCUPIED);
        container.markAsStored(storagePlace);

        Container savedContainer = containerDataService.save(container);

        movementService.recordOperationalMovement(
                savedContainer,
                null,
                null,
                storagePlace,
                savedContainer.getQuantity(),
                ContainerMovementType.PUTAWAY,
                actor
        );

        return ContainerResponse.from(savedContainer);
    }

    @Transactional
    public ContainerResponse mergeContainer(String sourceContainerNumber, MergeContainerRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        containerValidationService.validateMergeContainerRequest(actor, request);

        Container sourceContainer = containerDataService.getByContainerNumberOrThrow(sourceContainerNumber);
        Container targetContainer = containerDataService.getByContainerNumberOrThrow(
                request.targetContainerNumber().trim()
        );

        containerValidationService.validateContainersCanBeMerged(sourceContainer, targetContainer);

        int mergedQuantity = targetContainer.getQuantity() + sourceContainer.getQuantity();

        BigDecimal mergedWeightKg = dimensionCalculationService.calculateWeightKg(
                targetContainer.getArticle(),
                mergedQuantity
        );

        Integer mergedHeightMm = dimensionCalculationService.calculateHeightMm(
                targetContainer.getArticle(),
                mergedQuantity
        );

        containerValidationService.validateContainerFitsCurrentStoragePlace(
                targetContainer,
                mergedWeightKg,
                mergedHeightMm
        );

        targetContainer.setQuantity(mergedQuantity);
        targetContainer.setWeightKg(mergedWeightKg);
        targetContainer.setHeightMm(mergedHeightMm);

        int movedQuantity = sourceContainer.getQuantity();
        StoragePlace targetStoragePlace = targetContainer.getCurrentStoragePlace();
        sourceContainer.markAsMergedInto(targetContainer);

        containerDataService.save(targetContainer);
        Container savedSourceContainer = containerDataService.save(sourceContainer);

        movementService.recordOperationalMovement(
                savedSourceContainer,
                targetContainer,
                null,
                targetStoragePlace,
                movedQuantity,
                ContainerMovementType.MERGE,
                actor
        );

        return ContainerResponse.from(savedSourceContainer);
    }

    @Transactional
    public ContainerResponse removeContainer(String containerNumber) {
        User actor = authenticatedUserService.getCurrentUser();

        Container container = containerDataService.getByContainerNumberOrThrow(containerNumber);

        containerValidationService.validateRemoveContainer(actor, container);

        StoragePlace storagePlace = container.getCurrentStoragePlace();

        if (storagePlace != null) {
            storagePlace.setStatus(StoragePlaceStatus.AVAILABLE);
        }

        container.markAsRemoved();

        Container savedContainer = containerDataService.save(container);

        movementService.recordOperationalMovement(
                savedContainer,
                null,
                storagePlace,
                null,
                savedContainer.getQuantity(),
                ContainerMovementType.REMOVAL,
                actor
        );

        return ContainerResponse.from(savedContainer);
    }

    @Transactional
    public ReceiveContainersBatchResponse receiveContainersBatch(ReceiveContainersBatchRequest request) {
        User actor = authenticatedUserService.getCurrentUser();

        containerValidationService.validateReceiveContainersBatchRequest(actor, request);

        List<Container> containers = request.containers()
                .stream()
                .map(this::toContainer)
                .toList();

        List<Container> savedContainers = containerDataService.saveOnlyNew(containers);

        List<ContainerResponse> responses = savedContainers
                .stream()
                .map(ContainerResponse::from)
                .toList();

        return new ReceiveContainersBatchResponse(
                request.containers().size(),
                savedContainers.size(),
                responses
        );
    }

    private Container toContainer(ReceiveContainerRequest request) {
        Warehouse warehouse = containerDataService.getWarehouseByIdOrThrow(request.warehouseId());

        Article article = articleDataService.getByArticleNumberOrThrow(
                request.articleNumber().trim()
        );

        containerValidationService.validateContainerQuantityFitsArticle(
                article,
                request.quantity()
        );

        return Container.builder()
                .warehouse(warehouse)
                .containerNumber(request.containerNumber().trim())
                .article(article)
                .quantity(request.quantity())
                .weightKg(request.weightKg())
                .heightMm(request.heightMm())
                .status(ContainerStatus.WAITING_FOR_PLACEMENT)
                .build();
    }
}
