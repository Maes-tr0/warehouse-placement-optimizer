package com.nikitaopara.warehouseoptimizer.warehouse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "rack_bays",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rack_bays_row_code",
                        columnNames = {"rack_row_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_rack_bays_row_bay_number",
                        columnNames = {"rack_row_id", "bay_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RackBay {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_rack_bay")
    @SequenceGenerator(
            name = "seq_rack_bay",
            sequenceName = "seq_rack_bay",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_row_id", nullable = false)
    private RackRow rackRow;

    @OneToMany(
            mappedBy = "rackBay",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<RackLevel> rackLevels = new ArrayList<>();

    @Column(nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "bay_number", nullable = false)
    private Integer bayNumber;

    @Column(name = "positions_per_level", nullable = false)
    private Integer positionsPerLevel;

    @Column(name = "beam_length_mm", nullable = false)
    private Integer beamLengthMm;

    @Column(name = "max_bay_load_kg", nullable = false)
    private Integer maxBayLoadKg;

    @Column(name = "access_x_mm", nullable = false)
    private Integer accessXMm;

    @Column(name = "access_y_mm", nullable = false)
    private Integer accessYMm;

    @Column(name = "distance_from_aisle_start_mm", nullable = false)
    private Integer distanceFromAisleStartMm;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addRackLevel(RackLevel rackLevel) {
        if (rackLevel == null) {
            return;
        }

        this.rackLevels.add(rackLevel);
        rackLevel.setRackBay(this);
        rackLevel.setWarehouse(this.warehouse);
    }

    public void removeRackLevel(RackLevel rackLevel) {
        if (rackLevel == null) {
            return;
        }

        this.rackLevels.remove(rackLevel);
        rackLevel.setRackBay(null);
        rackLevel.setWarehouse(null);
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;

        if (rackLevels != null) {
            rackLevels.forEach(rackLevel -> rackLevel.setWarehouse(warehouse));
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

    private String getRackRowCode() {
        return rackRow != null ? rackRow.getCode() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof RackBay rackBay)) {
            return false;
        }

        return Objects.equals(getWarehouseCode(), rackBay.getWarehouseCode())
                && Objects.equals(getRackRowCode(), rackBay.getRackRowCode())
                && Objects.equals(code, rackBay.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWarehouseCode(), getRackRowCode(), code);
    }

    @Override
    public String toString() {
        return "RackBay{" +
                "id=" + id +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", rackRowCode='" + getRackRowCode() + '\'' +
                ", code='" + code + '\'' +
                ", bayNumber=" + bayNumber +
                ", positionsPerLevel=" + positionsPerLevel +
                ", beamLengthMm=" + beamLengthMm +
                ", maxBayLoadKg=" + maxBayLoadKg +
                ", accessXMm=" + accessXMm +
                ", accessYMm=" + accessYMm +
                ", distanceFromAisleStartMm=" + distanceFromAisleStartMm +
                ", version=" + version +
                ", rackLevelCount=" + (rackLevels != null ? rackLevels.size() : 0) +
                '}';
    }
}