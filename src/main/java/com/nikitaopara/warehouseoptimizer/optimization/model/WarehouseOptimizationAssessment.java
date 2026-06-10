package com.nikitaopara.warehouseoptimizer.optimization.model;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "warehouse_optimization_assessments",
        indexes = {
                @Index(
                        name = "idx_warehouse_optimization_assessments_latest",
                        columnList = "warehouse_id, analyzed_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseOptimizationAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_warehouse_optimization_assessment")
    @SequenceGenerator(
            name = "seq_warehouse_optimization_assessment",
            sequenceName = "seq_warehouse_optimization_assessment",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OptimizationAssessmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_trigger", nullable = false, length = 50)
    private OptimizationAssessmentTrigger trigger;

    @Column(name = "score_percent", precision = 5, scale = 2)
    private BigDecimal scorePercent;

    @Column(name = "threshold_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal thresholdPercent;

    @Column(name = "weighted_average_distance_mm", precision = 14, scale = 2)
    private BigDecimal weightedAverageDistanceMm;

    @Column(name = "lookback_start", nullable = false)
    private LocalDateTime lookbackStart;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @Column(name = "demand_observation_count", nullable = false)
    private Integer demandObservationCount;

    @Column(name = "analyzed_container_count", nullable = false)
    private Integer analyzedContainerCount;

    @Column(name = "demand_matched_container_count", nullable = false)
    private Integer demandMatchedContainerCount;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
