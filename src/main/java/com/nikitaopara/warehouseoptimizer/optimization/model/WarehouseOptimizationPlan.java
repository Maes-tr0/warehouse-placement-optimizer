package com.nikitaopara.warehouseoptimizer.optimization.model;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "warehouse_optimization_plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_warehouse_optimization_plans_code",
                        columnNames = "code"
                ),
                @UniqueConstraint(
                        name = "uk_warehouse_optimization_plans_assessment",
                        columnNames = "assessment_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseOptimizationPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_warehouse_optimization_plan")
    @SequenceGenerator(
            name = "seq_warehouse_optimization_plan",
            sequenceName = "seq_warehouse_optimization_plan",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false, updatable = false, length = 100)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false, unique = true)
    private WarehouseOptimizationAssessment assessment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OptimizationPlanStatus status;

    @Column(name = "initial_score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal initialScorePercent;

    @Column(name = "target_score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetScorePercent;

    @Column(name = "projected_score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal projectedScorePercent;

    @Column(name = "estimated_time_saving_seconds", nullable = false)
    private Long estimatedTimeSavingSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    @Builder.Default
    private List<WarehouseRelocationStep> steps = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addStep(WarehouseRelocationStep step) {
        steps.add(step);
        step.setPlan(this);
    }

    public void approve(User actor) {
        status = OptimizationPlanStatus.APPROVED;
        approvedBy = actor;
        approvedAt = LocalDateTime.now();

        if (!steps.isEmpty()) {
            steps.getFirst().markReady();
        }
    }

    public void markInProgress() {
        status = OptimizationPlanStatus.IN_PROGRESS;
    }

    public void markCompleted() {
        status = OptimizationPlanStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }

    public void cancel() {
        status = OptimizationPlanStatus.CANCELLED;
        steps.stream()
                .filter(step -> step.getStatus() != RelocationStepStatus.COMPLETED)
                .forEach(WarehouseRelocationStep::cancel);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = OptimizationPlanStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
