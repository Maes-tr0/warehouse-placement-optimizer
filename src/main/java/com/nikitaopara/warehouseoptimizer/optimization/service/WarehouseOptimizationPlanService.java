package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.auth.service.AuthenticatedUserService;
import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.demand.model.OrderDemandItem;
import com.nikitaopara.warehouseoptimizer.demand.repository.OrderDemandItemRepository;
import com.nikitaopara.warehouseoptimizer.optimization.config.OptimizationProperties;
import com.nikitaopara.warehouseoptimizer.optimization.dto.RelocationStepResponse;
import com.nikitaopara.warehouseoptimizer.optimization.dto.WarehouseOptimizationPlanResponse;
import com.nikitaopara.warehouseoptimizer.optimization.model.*;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationAssessmentRepository;
import com.nikitaopara.warehouseoptimizer.optimization.repository.WarehouseOptimizationPlanRepository;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.ContainerStatus;
import com.nikitaopara.warehouseoptimizer.putaway.container.repository.ContainerRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseOptimizationPlanService {

    private static final Set<OptimizationPlanStatus> ACTIVE_STATUSES = Set.of(
            OptimizationPlanStatus.DRAFT,
            OptimizationPlanStatus.APPROVED,
            OptimizationPlanStatus.IN_PROGRESS
    );

    private final AuthenticatedUserService authenticatedUserService;
    private final WarehouseOptimizationAssessmentRepository assessmentRepository;
    private final WarehouseOptimizationPlanRepository planRepository;
    private final OrderDemandItemRepository orderDemandItemRepository;
    private final ContainerRepository containerRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final SeasonalDemandModel seasonalDemandModel;
    private final WarehouseRelocationPlanner relocationPlanner;
    private final OptimizationProperties properties;

    @Transactional
    public WarehouseOptimizationPlanResponse createPlan(Long assessmentId) {
        User actor = authenticatedUserService.getCurrentUser();
        validateAdmin(actor);

        WarehouseOptimizationAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Optimization assessment not found: " + assessmentId
                ));

        validateAssessmentCanCreatePlan(assessment);

        Long warehouseId = assessment.getWarehouse().getId();

        if (planRepository.existsByWarehouseIdAndStatusIn(warehouseId, ACTIVE_STATUSES)) {
            throw new IllegalArgumentException(
                    "Warehouse already has an active optimization plan"
            );
        }

        List<OrderDemandItem> demandItems = orderDemandItemRepository
                .findByWarehouseIdAndOrderDemandOrderDateTimeBetween(
                        warehouseId,
                        assessment.getLookbackStart(),
                        assessment.getAnalyzedAt()
                );
        List<Container> containers = containerRepository.findByWarehouseIdAndStatus(
                warehouseId,
                ContainerStatus.STORED
        );
        List<StoragePlace> places = storagePlaceRepository
                .findByWarehouseIdOrderByDistanceFromEntryMmAsc(warehouseId);
        Map<Long, ArticleDemandScore> demandByArticle = seasonalDemandModel.calculate(
                demandItems.stream().map(this::toDemandObservation).toList(),
                assessment.getAnalyzedAt().toLocalDate(),
                properties.getRecencyHalfLifeDays(),
                properties.getSeasonalWindowDays()
        );
        RelocationPlanDraft draft = relocationPlanner.createPlan(
                containers,
                places,
                demandByArticle
        );

        if (draft.steps().isEmpty()) {
            throw new IllegalArgumentException(
                    "No safe relocation steps can improve this warehouse"
            );
        }

        WarehouseOptimizationPlan plan = buildPlan(
                assessment,
                actor,
                containers,
                places,
                draft
        );

        return toResponse(planRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public WarehouseOptimizationPlanResponse getPlan(String planCode) {
        return toResponse(getPlanByCodeOrThrow(planCode));
    }

    @Transactional
    public WarehouseOptimizationPlanResponse approvePlan(String planCode) {
        User actor = authenticatedUserService.getCurrentUser();
        validateAdmin(actor);

        WarehouseOptimizationPlan plan = getPlanByCodeOrThrow(planCode);

        if (plan.getStatus() != OptimizationPlanStatus.DRAFT) {
            throw new IllegalArgumentException("Only a draft optimization plan can be approved");
        }

        if (plan.getSteps().isEmpty()) {
            throw new IllegalArgumentException("Optimization plan has no relocation steps");
        }

        plan.approve(actor);

        return toResponse(planRepository.save(plan));
    }

    @Transactional
    public WarehouseOptimizationPlanResponse cancelPlan(String planCode) {
        User actor = authenticatedUserService.getCurrentUser();
        validateAdmin(actor);

        WarehouseOptimizationPlan plan = getPlanByCodeOrThrow(planCode);

        if (plan.getStatus() != OptimizationPlanStatus.DRAFT
                && plan.getStatus() != OptimizationPlanStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Only draft or approved optimization plan can be cancelled"
            );
        }

        plan.cancel();

        return toResponse(planRepository.save(plan));
    }

    private WarehouseOptimizationPlan buildPlan(
            WarehouseOptimizationAssessment assessment,
            User actor,
            List<Container> containers,
            List<StoragePlace> places,
            RelocationPlanDraft draft
    ) {
        Map<Long, Container> containersById = containers.stream()
                .collect(Collectors.toMap(Container::getId, Function.identity()));
        Map<Long, StoragePlace> placesById = places.stream()
                .collect(Collectors.toMap(StoragePlace::getId, Function.identity()));
        WarehouseOptimizationPlan plan = WarehouseOptimizationPlan.builder()
                .code("OPT-" + UUID.randomUUID())
                .warehouse(assessment.getWarehouse())
                .assessment(assessment)
                .status(OptimizationPlanStatus.DRAFT)
                .initialScorePercent(assessment.getScorePercent())
                .targetScorePercent(properties.getTargetPercent())
                .projectedScorePercent(draft.projectedScorePercent())
                .estimatedTimeSavingSeconds(draft.estimatedTimeSavingSeconds())
                .createdBy(actor)
                .build();

        int sequenceNumber = 1;

        for (PlannedRelocationStep plannedStep : draft.steps()) {
            WarehouseRelocationStep step = WarehouseRelocationStep.builder()
                    .sequenceNumber(sequenceNumber++)
                    .type(plannedStep.type())
                    .status(RelocationStepStatus.PENDING)
                    .sourceContainer(requireContainer(
                            containersById,
                            plannedStep.sourceContainerId()
                    ))
                    .targetContainer(optionalContainer(
                            containersById,
                            plannedStep.targetContainerId()
                    ))
                    .fromStoragePlace(optionalPlace(
                            placesById,
                            plannedStep.fromStoragePlaceId()
                    ))
                    .toStoragePlace(optionalPlace(
                            placesById,
                            plannedStep.toStoragePlaceId()
                    ))
                    .estimatedTimeSavingSeconds(plannedStep.estimatedTimeSavingSeconds())
                    .reason(plannedStep.reason())
                    .build();
            plan.addStep(step);
        }

        return plan;
    }

    private void validateAssessmentCanCreatePlan(WarehouseOptimizationAssessment assessment) {
        if (assessment.getStatus() != OptimizationAssessmentStatus.OPTIMIZATION_RECOMMENDED) {
            throw new IllegalArgumentException(
                    "Optimization plan requires an OPTIMIZATION_RECOMMENDED assessment"
            );
        }

        if (assessment.getScorePercent() == null) {
            throw new IllegalArgumentException("Optimization assessment has no score");
        }

        if (assessment.getScorePercent().compareTo(properties.getThresholdPercent()) >= 0) {
            throw new IllegalArgumentException(
                    "Warehouse score is not below the optimization threshold"
            );
        }

        if (planRepository.existsByAssessmentId(assessment.getId())) {
            throw new IllegalArgumentException(
                    "Optimization plan already exists for this assessment"
            );
        }
    }

    private void validateAdmin(User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() != Role.ROOT_ADMIN && actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "Only ADMIN or ROOT_ADMIN can manage optimization plans"
            );
        }
    }

    private WarehouseOptimizationPlan getPlanByCodeOrThrow(String planCode) {
        return planRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Optimization plan not found: " + planCode
                ));
    }

    private DemandObservation toDemandObservation(OrderDemandItem item) {
        return new DemandObservation(
                item.getArticle().getId(),
                item.getOrderDemand().getId(),
                item.getOrderDemand().getOrderDateTime(),
                item.getQuantity()
        );
    }

    private Container requireContainer(Map<Long, Container> containers, Long id) {
        Container container = containers.get(id);

        if (container == null) {
            throw new IllegalStateException("Planned container is unavailable: " + id);
        }

        return container;
    }

    private Container optionalContainer(Map<Long, Container> containers, Long id) {
        return id == null ? null : requireContainer(containers, id);
    }

    private StoragePlace optionalPlace(Map<Long, StoragePlace> places, Long id) {
        if (id == null) {
            return null;
        }

        StoragePlace place = places.get(id);

        if (place == null) {
            throw new IllegalStateException("Planned storage place is unavailable: " + id);
        }

        return place;
    }

    private WarehouseOptimizationPlanResponse toResponse(WarehouseOptimizationPlan plan) {
        List<RelocationStepResponse> steps = plan.getSteps().stream()
                .map(this::toStepResponse)
                .toList();
        int completedSteps = (int) steps.stream()
                .filter(step -> step.status() == RelocationStepStatus.COMPLETED)
                .count();

        return new WarehouseOptimizationPlanResponse(
                plan.getCode(),
                plan.getWarehouse().getId(),
                plan.getAssessment().getId(),
                plan.getStatus(),
                plan.getInitialScorePercent(),
                plan.getTargetScorePercent(),
                plan.getProjectedScorePercent(),
                plan.getEstimatedTimeSavingSeconds(),
                steps.size(),
                completedSteps,
                plan.getApprovedAt(),
                plan.getCompletedAt(),
                plan.getCreatedAt(),
                steps
        );
    }

    private RelocationStepResponse toStepResponse(WarehouseRelocationStep step) {
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
