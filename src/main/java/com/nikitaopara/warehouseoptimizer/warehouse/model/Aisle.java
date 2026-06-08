package com.nikitaopara.warehouseoptimizer.warehouse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "aisles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_aisles_warehouse_code",
                        columnNames = {"warehouse_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_aisles_warehouse_sequence",
                        columnNames = {"warehouse_id", "sequence_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aisle {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_aisle")
    @SequenceGenerator(
            name = "seq_aisle",
            sequenceName = "seq_aisle",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @OneToMany(
            mappedBy = "aisle",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<RackRow> rackRows = new ArrayList<>();

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "width_mm", nullable = false)
    private Integer widthMm;

    @Column(name = "length_mm", nullable = false)
    private Integer lengthMm;

    @Column(name = "entry_x_mm", nullable = false)
    private Integer entryXMm;

    @Column(name = "entry_y_mm", nullable = false)
    private Integer entryYMm;

    @Column(name = "distance_from_entry_mm", nullable = false)
    private Integer distanceFromEntryMm;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addRackRow(RackRow rackRow) {
        if (rackRow == null) {
            return;
        }

        this.rackRows.add(rackRow);
        rackRow.setAisle(this);
        rackRow.setWarehouse(this.warehouse);
    }

    public void removeRackRow(RackRow rackRow) {
        if (rackRow == null) {
            return;
        }

        this.rackRows.remove(rackRow);
        rackRow.setAisle(null);
        rackRow.setWarehouse(null);
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;

        if (rackRows != null) {
            rackRows.forEach(rackRow -> rackRow.setWarehouse(warehouse));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Aisle aisle)) {
            return false;
        }

        return Objects.equals(getWarehouseCode(), aisle.getWarehouseCode())
                && Objects.equals(code, aisle.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWarehouseCode(), code);
    }

    @Override
    public String toString() {
        return "Aisle{" +
                "id=" + id +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", code='" + code + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                ", widthMm=" + widthMm +
                ", lengthMm=" + lengthMm +
                ", entryXMm=" + entryXMm +
                ", entryYMm=" + entryYMm +
                ", distanceFromEntryMm=" + distanceFromEntryMm +
                ", version=" + version +
                ", rackRowCount=" + (rackRows != null ? rackRows.size() : 0) +
                '}';
    }

    private String getWarehouseCode() {
        return warehouse != null ? warehouse.getCode() : null;
    }
}