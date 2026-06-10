package com.nikitaopara.warehouseoptimizer.movement.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovement;
import com.nikitaopara.warehouseoptimizer.movement.model.ContainerMovementType;
import com.nikitaopara.warehouseoptimizer.movement.repository.ContainerMovementRepository;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationPlan;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseRelocationStep;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContainerMovementService {

    private final ContainerMovementRepository movementRepository;

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
        ContainerMovement movement = ContainerMovement.builder()
                .warehouse(plan.getWarehouse())
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

        return movementRepository.save(movement);
    }
}
