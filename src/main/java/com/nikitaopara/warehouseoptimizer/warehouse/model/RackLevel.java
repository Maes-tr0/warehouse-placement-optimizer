package com.nikitaopara.warehouseoptimizer.warehouse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "rack_levels",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rack_levels_bay_code",
                        columnNames = {"rack_bay_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_rack_levels_bay_level_number",
                        columnNames = {"rack_bay_id", "level_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RackLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_rack_level")
    @SequenceGenerator(
            name = "seq_rack_level",
            sequenceName = "seq_rack_level",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_bay_id", nullable = false)
    private RackBay rackBay;

    @OneToMany(
            mappedBy = "rackLevel",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<StoragePlace> storagePlaces = new ArrayList<>();

    @Column(nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;

    @Column(name = "clear_height_mm", nullable = false)
    private Integer clearHeightMm;

    @Column(name = "height_from_floor_mm", nullable = false)
    private Integer heightFromFloorMm;

    @Column(name = "max_level_load_kg", nullable = false)
    private Integer maxLevelLoadKg;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addStoragePlace(StoragePlace storagePlace) {
        if (storagePlace == null) {
            return;
        }

        this.storagePlaces.add(storagePlace);

        storagePlace.setRackLevel(this);
        storagePlace.setRackBay(this.rackBay);
        storagePlace.setWarehouse(this.warehouse);

        if (this.rackBay != null) {
            storagePlace.setRackRow(this.rackBay.getRackRow());
        }
    }

    public void removeStoragePlace(StoragePlace storagePlace) {
        if (storagePlace == null) {
            return;
        }

        this.storagePlaces.remove(storagePlace);

        storagePlace.setRackLevel(null);
        storagePlace.setRackBay(null);
        storagePlace.setRackRow(null);
        storagePlace.setWarehouse(null);
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;

        if (storagePlaces != null) {
            storagePlaces.forEach(storagePlace -> storagePlace.setWarehouse(warehouse));
        }
    }

    public void setRackBay(RackBay rackBay) {
        this.rackBay = rackBay;

        if (rackBay != null) {
            this.warehouse = rackBay.getWarehouse();
        }

        if (storagePlaces != null) {
            storagePlaces.forEach(storagePlace -> {
                storagePlace.setRackBay(rackBay);

                if (rackBay != null) {
                    storagePlace.setWarehouse(rackBay.getWarehouse());
                    storagePlace.setRackRow(rackBay.getRackRow());
                }
            });
        }
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

    private String getWarehouseCode() {
        return warehouse != null ? warehouse.getCode() : null;
    }

    private String getRackBayCode() {
        return rackBay != null ? rackBay.getCode() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof RackLevel rackLevel)) {
            return false;
        }

        return Objects.equals(getWarehouseCode(), rackLevel.getWarehouseCode())
                && Objects.equals(getRackBayCode(), rackLevel.getRackBayCode())
                && Objects.equals(code, rackLevel.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWarehouseCode(), getRackBayCode(), code);
    }

    @Override
    public String toString() {
        return "RackLevel{" +
                "id=" + id +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", rackBayCode='" + getRackBayCode() + '\'' +
                ", code='" + code + '\'' +
                ", levelNumber=" + levelNumber +
                ", clearHeightMm=" + clearHeightMm +
                ", heightFromFloorMm=" + heightFromFloorMm +
                ", maxLevelLoadKg=" + maxLevelLoadKg +
                ", version=" + version +
                ", storagePlaceCount=" + (storagePlaces != null ? storagePlaces.size() : 0) +
                '}';
    }
}