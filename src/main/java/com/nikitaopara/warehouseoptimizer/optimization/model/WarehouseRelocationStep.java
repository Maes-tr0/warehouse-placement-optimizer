package com.nikitaopara.warehouseoptimizer.optimization.model;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "warehouse_relocation_steps",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_warehouse_relocation_steps_plan_sequence",
                        columnNames = {"plan_id", "sequence_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseRelocationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_warehouse_relocation_step")
    @SequenceGenerator(
            name = "seq_warehouse_relocation_step",
            sequenceName = "seq_warehouse_relocation_step",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private WarehouseOptimizationPlan plan;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 50)
    private RelocationStepType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RelocationStepStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_container_id", nullable = false)
    private Container sourceContainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_container_id")
    private Container targetContainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_storage_place_id")
    private StoragePlace fromStoragePlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_storage_place_id")
    private StoragePlace toStoragePlace;

    @Column(name = "estimated_time_saving_seconds", nullable = false)
    private Long estimatedTimeSavingSeconds;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id")
    private User completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void markReady() {
        status = RelocationStepStatus.READY;
    }

    public void markCompleted(User actor) {
        status = RelocationStepStatus.COMPLETED;
        completedBy = actor;
        completedAt = LocalDateTime.now();
    }

    public void cancel() {
        status = RelocationStepStatus.CANCELLED;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = RelocationStepStatus.PENDING;
        }
    }
}
