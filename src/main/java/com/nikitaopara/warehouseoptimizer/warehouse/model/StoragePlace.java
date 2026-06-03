package com.nikitaopara.warehouseoptimizer.warehouse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "storage_places",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_storage_places_warehouse_code",
                        columnNames = {"warehouse_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_storage_places_level_position",
                        columnNames = {"rack_level_id", "position_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoragePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_storage_place")
    @SequenceGenerator(
            name = "seq_storage_place",
            sequenceName = "seq_storage_place",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_row_id", nullable = false)
    private RackRow rackRow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_bay_id", nullable = false)
    private RackBay rackBay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_level_id", nullable = false)
    private RackLevel rackLevel;

    @Column(nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "position_number", nullable = false)
    private Integer positionNumber;

    @Column(name = "max_weight_kg", nullable = false)
    private Integer maxWeightKg;

    @Column(name = "max_height_mm", nullable = false)
    private Integer maxHeightMm;

    @Column(name = "distance_from_aisle_start_mm", nullable = false)
    private Integer distanceFromAisleStartMm;

    @Column(name = "distance_from_entry_mm", nullable = false)
    private Integer distanceFromEntryMm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StoragePlaceStatus status;

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
            this.status = StoragePlaceStatus.AVAILABLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String getWarehouseCode() {
        return warehouse != null ? warehouse.getCode() : null;
    }

    private String getRackRowCode() {
        return rackRow != null ? rackRow.getCode() : null;
    }

    private String getRackBayCode() {
        return rackBay != null ? rackBay.getCode() : null;
    }

    private String getRackLevelCode() {
        return rackLevel != null ? rackLevel.getCode() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof StoragePlace storagePlace)) {
            return false;
        }

        return Objects.equals(getWarehouseCode(), storagePlace.getWarehouseCode())
                && Objects.equals(code, storagePlace.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWarehouseCode(), code);
    }

    @Override
    public String toString() {
        return "StoragePlace{" +
                "id=" + id +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", rackRowCode='" + getRackRowCode() + '\'' +
                ", rackBayCode='" + getRackBayCode() + '\'' +
                ", rackLevelCode='" + getRackLevelCode() + '\'' +
                ", code='" + code + '\'' +
                ", positionNumber=" + positionNumber +
                ", maxWeightKg=" + maxWeightKg +
                ", maxHeightMm=" + maxHeightMm +
                ", distanceFromAisleStartMm=" + distanceFromAisleStartMm +
                ", distanceFromEntryMm=" + distanceFromEntryMm +
                ", status=" + status +
                ", version=" + version +
                '}';
    }
}