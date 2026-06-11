package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovementType;
import com.nikitaopara.warehouseoptimizer.movement.service.ContainerMovementService;
import com.nikitaopara.warehouseoptimizer.optimization.dto.CompleteRelocationStepRequest;
import com.nikitaopara.warehouseoptimizer.optimization.dto.RelocationExecutionResponse;
import com.nikitaopara.warehouseoptimizer.optimization.dto.RelocationStepResponse;
import com.nikitaopara.warehouseoptimizer.optimization.model.*;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationPlanRepository;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.service.ContainerDimensionCalculationService;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlaceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WarehouseRelocationExecutionService {

    private final AuthenticatedUserService authenticatedUserService;
    private final WarehouseOptimizationPlanRepository planRepository;
    private final ContainerDimensionCalculationService dimensionCalculationService;
    private final ContainerMovementService movementService;

    @Transactional(readOnly = true)
    public RelocationStepResponse getCurrentStep(String planCode) {
        WarehouseOptimizationPlan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Optimization plan not found: " + planCode
                ));

        return toResponse(getReadyStep(plan));
    }

    @Transactional
    public RelocationExecutionResponse completeCurrentStep(
            String planCode,
            CompleteRelocationStepRequest request
    ) {
        User actor = authenticatedUserService.getCurrentUser();
        validateOperatorOrAdmin(actor);

        WarehouseOptimizationPlan plan = planRepository.findForUpdateByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Optimization plan not found: " + planCode
                ));
        validatePlanCanBeExecuted(plan);

        WarehouseRelocationStep step = getReadyStep(plan);
        validateStepReservations(plan, step);
        validateSourceScan(step, request);

        if (step.getType() == RelocationStepType.MERGE) {
            executeMerge(plan, step, request, actor);
        } else {
            executeMove(plan, step, request, actor);
        }

        step.markCompleted(actor);

        if (plan.getStatus() == OptimizationPlanStatus.APPROVED) {
            plan.markInProgress();
        }

        WarehouseRelocationStep nextStep = plan.getSteps().stream()
                .filter(candidate -> candidate.getStatus() == RelocationStepStatus.PENDING)
                .min(Comparator.comparingInt(WarehouseRelocationStep::getSequenceNumber))
                .orElse(null);

        if (nextStep == null) {
            plan.markCompleted();
            releaseResources(plan);
        } else {
            nextStep.markReady();
        }

        planRepository.save(plan);

        return new RelocationExecutionResponse(
                plan.getCode(),
                plan.getStatus(),
                toResponse(step),
                nextStep == null ? null : toResponse(nextStep)
        );
    }

    private void executeMove(
            WarehouseOptimizationPlan plan,
            WarehouseRelocationStep step,
            CompleteRelocationStepRequest request,
            User actor
    ) {
        Container source = step.getSourceContainer();
        StoragePlace from = step.getFromStoragePlace();
        StoragePlace to = step.getToStoragePlace();

        if (!StringUtils.hasText(request.targetStoragePlaceCode())) {
            throw new IllegalArgumentException("Target storage place scan is required");
        }

        if (to == null || !to.getCode().equals(request.targetStoragePlaceCode().trim())) {
            throw new IllegalArgumentException("Scanned target storage place does not match the plan");
        }

        validateCurrentStoragePlace(source, from);

        if (to.getStatus() != StoragePlaceStatus.AVAILABLE) {
            throw new IllegalArgumentException("Target storage place is not available");
        }

        validateFits(source, to);

        int movedQuantity = source.getQuantity();
        from.setStatus(StoragePlaceStatus.AVAILABLE);
        to.setStatus(StoragePlaceStatus.OCCUPIED);
        source.relocateTo(to);

        ContainerMovementType movementType = step.getType() == RelocationStepType.TEMPORARY_MOVE
                ? ContainerMovementType.TEMPORARY_RELOCATION
                : ContainerMovementType.RELOCATION;

        movementService.recordOptimizationMovement(
                plan,
                step,
                source,
                null,
                from,
                to,
                movedQuantity,
                movementType,
                actor
        );
    }

    private void executeMerge(
            WarehouseOptimizationPlan plan,
            WarehouseRelocationStep step,
            CompleteRelocationStepRequest request,
            User actor
    ) {
        Container source = step.getSourceContainer();
        Container target = step.getTargetContainer();
        StoragePlace sourcePlace = step.getFromStoragePlace();
        StoragePlace targetPlace = step.getToStoragePlace();

        if (!StringUtils.hasText(request.targetContainerNumber())) {
            throw new IllegalArgumentException("Target container scan is required for merge");
        }

        if (target == null
                || !target.getContainerNumber().equals(request.targetContainerNumber().trim())) {
            throw new IllegalArgumentException("Scanned target container does not match the plan");
        }

        validateCurrentStoragePlace(source, sourcePlace);
        validateCurrentStoragePlace(target, targetPlace);

        if (!source.isStored() || !target.isStored()) {
            throw new IllegalArgumentException("Both merge containers must be stored");
        }

        if (!Objects.equals(source.getWarehouse().getId(), target.getWarehouse().getId())) {
            throw new IllegalArgumentException("Merge containers belong to different warehouses");
        }

        if (!Objects.equals(source.getArticle().getId(), target.getArticle().getId())) {
            throw new IllegalArgumentException("Only containers of the same article can be merged");
        }

        int sourceQuantity = source.getQuantity();
        int mergedQuantity = sourceQuantity + target.getQuantity();

        if (!target.getArticle().canFitQuantity(mergedQuantity)) {
            throw new IllegalArgumentException("Merged quantity exceeds pallet capacity");
        }

        BigDecimal mergedWeight = dimensionCalculationService.calculateWeightKg(
                target.getArticle(),
                mergedQuantity
        );
        int mergedHeight = dimensionCalculationService.calculateHeightMm(
                target.getArticle(),
                mergedQuantity
        );

        if (mergedWeight.compareTo(BigDecimal.valueOf(targetPlace.getMaxWeightKg())) > 0
                || mergedHeight > targetPlace.getMaxHeightMm()) {
            throw new IllegalArgumentException("Merged container does not fit target storage place");
        }

        target.setQuantity(mergedQuantity);
        target.setWeightKg(mergedWeight);
        target.setHeightMm(mergedHeight);
        sourcePlace.setStatus(StoragePlaceStatus.AVAILABLE);
        source.markAsMergedInto(target);

        movementService.recordOptimizationMovement(
                plan,
                step,
                source,
                target,
                sourcePlace,
                targetPlace,
                sourceQuantity,
                ContainerMovementType.MERGE,
                actor
        );
    }

    private void validateSourceScan(
            WarehouseRelocationStep step,
            CompleteRelocationStepRequest request
    ) {
        if (request == null || !StringUtils.hasText(request.sourceContainerNumber())) {
            throw new IllegalArgumentException("Source container scan is required");
        }

        if (!step.getSourceContainer().getContainerNumber()
                .equals(request.sourceContainerNumber().trim())) {
            throw new IllegalArgumentException("Scanned source container does not match the plan");
        }
    }

    private void validateCurrentStoragePlace(Container container, StoragePlace expectedPlace) {
        if (expectedPlace == null
                || container.getCurrentStoragePlace() == null
                || !Objects.equals(
                        container.getCurrentStoragePlace().getId(),
                        expectedPlace.getId()
                )) {
            throw new IllegalArgumentException(
                    "Container is not located at the storage place expected by the plan"
            );
        }
    }

    private void validateFits(Container container, StoragePlace place) {
        if (container.getWeightKg().compareTo(BigDecimal.valueOf(place.getMaxWeightKg())) > 0) {
            throw new IllegalArgumentException("Container weight exceeds target place capacity");
        }

        if (container.getHeightMm() > place.getMaxHeightMm()) {
            throw new IllegalArgumentException("Container height exceeds target place capacity");
        }
    }

    private void validatePlanCanBeExecuted(WarehouseOptimizationPlan plan) {
        if (plan.getStatus() != OptimizationPlanStatus.APPROVED
                && plan.getStatus() != OptimizationPlanStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Only approved or in-progress optimization plan can be executed"
            );
        }
    }

    private void validateStepReservations(
            WarehouseOptimizationPlan plan,
            WarehouseRelocationStep step
    ) {
        String planCode = plan.getCode();
        validateContainerReservation(step.getSourceContainer(), planCode);

        if (step.getTargetContainer() != null) {
            validateContainerReservation(step.getTargetContainer(), planCode);
        }

        if (step.getFromStoragePlace() != null) {
            validatePlaceReservation(step.getFromStoragePlace(), planCode);
        }

        if (step.getToStoragePlace() != null) {
            validatePlaceReservation(step.getToStoragePlace(), planCode);
        }
    }

    private void validateContainerReservation(Container container, String planCode) {
        if (!container.isReservedForOptimizationPlan(planCode)) {
            throw new IllegalArgumentException(
                    "Container is not reserved for optimization plan: " + planCode
            );
        }
    }

    private void validatePlaceReservation(StoragePlace place, String planCode) {
        if (!place.isReservedForOptimizationPlan(planCode)) {
            throw new IllegalArgumentException(
                    "Storage place is not reserved for optimization plan: " + planCode
            );
        }
    }

    private void releaseResources(WarehouseOptimizationPlan plan) {
        plan.getSteps().forEach(step -> {
            step.getSourceContainer().releaseOptimizationReservation(plan.getCode());

            if (step.getTargetContainer() != null) {
                step.getTargetContainer().releaseOptimizationReservation(plan.getCode());
            }

            if (step.getFromStoragePlace() != null) {
                step.getFromStoragePlace().releaseOptimizationReservation(plan.getCode());
            }

            if (step.getToStoragePlace() != null) {
                step.getToStoragePlace().releaseOptimizationReservation(plan.getCode());
            }
        });
    }

    private void validateOperatorOrAdmin(User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() != Role.ROOT_ADMIN
                && actor.getRole() != Role.ADMIN
                && actor.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException(
                    "Only OPERATOR, ADMIN or ROOT_ADMIN can execute relocation steps"
            );
        }
    }

    private WarehouseRelocationStep getReadyStep(WarehouseOptimizationPlan plan) {
        return plan.getSteps().stream()
                .filter(step -> step.getStatus() == RelocationStepStatus.READY)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Optimization plan has no ready relocation step"
                ));
    }

    private RelocationStepResponse toResponse(WarehouseRelocationStep step) {
        Container source = step.getSourceContainer();
        Container target = step.getTargetContainer();

        return new RelocationStepResponse(
                step.getId(),
                step.getSequenceNumber(),
                step.getType(),
                step.getStatus(),
                source.getContainerNumber(),
                source.getArticle().getArticleNumber(),
                target == null ? null : target.getContainerNumber(),
                target == null ? null : target.getArticle().getArticleNumber(),
                step.getFromStoragePlace() == null
                        ? null
                        : step.getFromStoragePlace().getCode(),
                step.getToStoragePlace() == null
                        ? null
                        : step.getToStoragePlace().getCode(),
                step.getEstimatedTimeSavingSeconds(),
                step.getReason(),
                step.getCompletedAt()
        );
    }
}
