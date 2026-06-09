package com.nikitaopara.warehouseoptimizer.putaway.placement.model;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "placement_recommendations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_placement_recommendations_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_placement_recommendations_source_container_status",
                        columnList = "source_container_id, status"
                ),
                @Index(
                        name = "idx_placement_recommendations_source_container_type",
                        columnList = "source_container_id, recommendation_type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_placement_recommendation")
    @SequenceGenerator(
            name = "seq_placement_recommendation",
            sequenceName = "seq_placement_recommendation",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 100)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_container_id", nullable = false)
    private Container sourceContainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_container_id")
    private Container targetContainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_storage_place_id")
    private StoragePlace recommendedStoragePlace;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 50)
    private PlacementRecommendationType recommendationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlacementRecommendationStatus status;

    @Column(name = "distance_from_entry_mm")
    private Integer distanceFromEntryMm;

    @Column(name = "estimated_time_seconds")
    private Integer estimatedTimeSeconds;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal score;

    @Column(length = 500)
    private String reason;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = PlacementRecommendationStatus.SUGGESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isMergeRecommendation() {
        return recommendationType == PlacementRecommendationType.MERGE;
    }

    public boolean isPlaceRecommendation() {
        return recommendationType == PlacementRecommendationType.PLACE;
    }

    public boolean isSuggested() {
        return status == PlacementRecommendationStatus.SUGGESTED;
    }

    public boolean isAccepted() {
        return status == PlacementRecommendationStatus.ACCEPTED;
    }

    public boolean isRejected() {
        return status == PlacementRecommendationStatus.REJECTED;
    }

    public void accept() {
        this.status = PlacementRecommendationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = PlacementRecommendationStatus.REJECTED;
    }

    private String getWarehouseCode() {
        return warehouse != null ? warehouse.getCode() : null;
    }

    private String getSourceContainerNumber() {
        return sourceContainer != null ? sourceContainer.getContainerNumber() : null;
    }

    private String getTargetContainerNumber() {
        return targetContainer != null ? targetContainer.getContainerNumber() : null;
    }

    private String getRecommendedStoragePlaceCode() {
        return recommendedStoragePlace != null ? recommendedStoragePlace.getCode() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof PlacementRecommendation that)) {
            return false;
        }

        return code != null && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "PlacementRecommendation{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", sourceContainerNumber='" + getSourceContainerNumber() + '\'' +
                ", targetContainerNumber='" + getTargetContainerNumber() + '\'' +
                ", recommendedStoragePlaceCode='" + getRecommendedStoragePlaceCode() + '\'' +
                ", recommendationType=" + recommendationType +
                ", status=" + status +
                ", distanceFromEntryMm=" + distanceFromEntryMm +
                ", estimatedTimeSeconds=" + estimatedTimeSeconds +
                ", score=" + score +
                ", version=" + version +
                '}';
    }
}