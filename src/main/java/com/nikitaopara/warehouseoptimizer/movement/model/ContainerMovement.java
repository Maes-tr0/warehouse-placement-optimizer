package com.nikitaopara.warehouseoptimizer.movement.model;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseOptimizationPlan;
import com.nikitaopara.warehouseoptimizer.optimization.model.WarehouseRelocationStep;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "container_movements",
        indexes = {
                @Index(
                        name = "idx_container_movements_warehouse_performed_at",
                        columnList = "warehouse_id, performed_at"
                ),
                @Index(
                        name = "idx_container_movements_container_performed_at",
                        columnList = "container_id, performed_at"
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_container_movement")
    @SequenceGenerator(
            name = "seq_container_movement",
            sequenceName = "seq_container_movement",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false, updatable = false)
    private Container container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_container_id", updatable = false)
    private Container targetContainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_storage_place_id", updatable = false)
    private StoragePlace fromStoragePlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_storage_place_id", updatable = false)
    private StoragePlace toStoragePlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "optimization_plan_id", updatable = false)
    private WarehouseOptimizationPlan optimizationPlan;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relocation_step_id", unique = true, updatable = false)
    private WarehouseRelocationStep relocationStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, updatable = false, length = 50)
    private ContainerMovementType type;

    @Column(name = "container_number", nullable = false, updatable = false, length = 100)
    private String containerNumber;

    @Column(name = "article_number", nullable = false, updatable = false, length = 50)
    private String articleNumber;

    @Column(name = "target_container_number", updatable = false, length = 100)
    private String targetContainerNumber;

    @Column(name = "from_storage_place_code", updatable = false, length = 50)
    private String fromStoragePlaceCode;

    @Column(name = "to_storage_place_code", updatable = false, length = 50)
    private String toStoragePlaceCode;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id", nullable = false, updatable = false)
    private User performedBy;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }
}
