package com.nikitaopara.warehouseoptimizer.movement.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.eventing.model.ContainerMovementEventPayload;
import com.nikitaopara.warehouseoptimizer.eventing.model.WarehouseEventTopics;
import com.nikitaopara.warehouseoptimizer.eventing.service.DomainEventPublisher;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovement;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovementType;
import com.nikitaopara.warehouseoptimizer.movement.repository.ContainerMovementRepository;
import com.nikitaopara.warehouseoptimizer.observability.WarehouseBusinessMetrics;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationPlan;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseRelocationStep;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContainerMovementService {

    private final ContainerMovementRepository movementRepository;
    private final DomainEventPublisher eventPublisher;
    private final WarehouseBusinessMetrics businessMetrics;

    @Transactional
    public ContainerMovement recordOperationalMovement(
            Container source,
            Container target,
            StoragePlace from,
            StoragePlace to,
            int movedQuantity,
            ContainerMovementType type,
            User actor
    ) {
        return saveMovement(
                null,
                null,
                source,
                target,
                from,
                to,
                movedQuantity,
                type,
                actor
        );
    }

    @Transactional
    public ContainerMovement recordOptimizationMovement(
            WarehouseOptimizationPlan plan,
            WarehouseRelocationStep step,
            Container source,
            Container target,
            StoragePlace from,
            StoragePlace to,
            int movedQuantity,
            ContainerMovementType type,
            User actor
    ) {
        return saveMovement(
                plan,
                step,
                source,
                target,
                from,
                to,
                movedQuantity,
                type,
                actor
        );
    }

    @Transactional(readOnly = true)
    public List<ContainerMovement> getWarehouseHistory(Long warehouseId) {
        return movementRepository.findByWarehouseIdOrderByPerformedAtDesc(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<ContainerMovement> getContainerHistory(String containerNumber) {
        return movementRepository.findByContainerContainerNumberOrderByPerformedAtDesc(
                containerNumber
        );
    }

    private ContainerMovement saveMovement(
            WarehouseOptimizationPlan plan,
            WarehouseRelocationStep step,
            Container source,
            Container target,
            StoragePlace from,
            StoragePlace to,
            int movedQuantity,
            ContainerMovementType type,
            User actor
    ) {
        ContainerMovement movement = ContainerMovement.builder()
                .warehouse(source.getWarehouse())
                .container(source)
                .targetContainer(target)
                .fromStoragePlace(from)
                .toStoragePlace(to)
                .optimizationPlan(plan)
                .relocationStep(step)
                .type(type)
                .containerNumber(source.getContainerNumber())
                .articleNumber(source.getArticle().getArticleNumber())
                .targetContainerNumber(target == null ? null : target.getContainerNumber())
                .fromStoragePlaceCode(from == null ? null : from.getCode())
                .toStoragePlaceCode(to == null ? null : to.getCode())
                .quantity(movedQuantity)
                .performedBy(actor)
                .build();

        ContainerMovement saved = movementRepository.save(movement);
        eventPublisher.publish(
                WarehouseEventTopics.CONTAINER_MOVEMENTS,
                "container.movement.recorded",
                "ContainerMovement",
                saved.getId().toString(),
                saved.getWarehouse().getCode(),
                ContainerMovementEventPayload.from(saved)
        );
        businessMetrics.containerMovementRecorded(saved);

        return saved;
    }
}
