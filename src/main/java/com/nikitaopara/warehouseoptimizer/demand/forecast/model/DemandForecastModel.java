package com.nikitaopara.warehouseoptimizer.demand.forecast.model;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "demand_forecast_models",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_demand_forecast_models_code",
                        columnNames = "code"
                ),
                @UniqueConstraint(
                        name = "uk_demand_forecast_models_warehouse_version",
                        columnNames = {"warehouse_id", "version_number"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_demand_forecast_models_warehouse_status",
                        columnList = "warehouse_id, status, trained_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandForecastModel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_demand_forecast_model")
    @SequenceGenerator(
            name = "seq_demand_forecast_model",
            sequenceName = "seq_demand_forecast_model",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 100)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "version_number", nullable = false, updatable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DemandForecastModelStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_trigger", nullable = false, length = 50)
    private DemandForecastTrainingTrigger trainingTrigger;

    @Column(nullable = false, length = 100)
    private String algorithm;

    @Column(name = "feature_schema_version", nullable = false)
    private Integer featureSchemaVersion;

    @Column(name = "forecast_horizon_days", nullable = false)
    private Integer forecastHorizonDays;

    @Column(name = "training_start", nullable = false)
    private LocalDate trainingStart;

    @Column(name = "training_end", nullable = false)
    private LocalDate trainingEnd;

    @Column(name = "validation_start", nullable = false)
    private LocalDate validationStart;

    @Column(name = "validation_end", nullable = false)
    private LocalDate validationEnd;

    @Column(name = "data_cutoff", nullable = false)
    private LocalDate dataCutoff;

    @Column(name = "observation_count", nullable = false)
    private Integer observationCount;

    @Column(name = "article_count", nullable = false)
    private Integer articleCount;

    @Column(name = "training_sample_count", nullable = false)
    private Integer trainingSampleCount;

    @Column(name = "validation_sample_count", nullable = false)
    private Integer validationSampleCount;

    @Column(name = "model_mae")
    private Double modelMae;

    @Column(name = "baseline_mae")
    private Double baselineMae;

    @Column(name = "model_rmse")
    private Double modelRmse;

    @Column(name = "model_r2")
    private Double modelR2;

    @Column(name = "improvement_percent")
    private Double improvementPercent;

    @Column(name = "model_artifact")
    private byte[] modelArtifact;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "trained_at")
    private LocalDateTime trainedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void activate(byte[] artifact, LocalDateTime completedAt) {
        this.status = DemandForecastModelStatus.ACTIVE;
        this.modelArtifact = artifact;
        this.trainedAt = completedAt;
        this.errorMessage = null;
    }

    public void reject(LocalDateTime completedAt) {
        this.status = DemandForecastModelStatus.REJECTED;
        this.modelArtifact = null;
        this.trainedAt = completedAt;
    }

    public void fail(String message, LocalDateTime completedAt) {
        this.status = DemandForecastModelStatus.FAILED;
        this.modelArtifact = null;
        this.errorMessage = message;
        this.trainedAt = completedAt;
    }

    public void supersede() {
        this.status = DemandForecastModelStatus.SUPERSEDED;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
