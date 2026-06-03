package com.nikitaopara.warehouseoptimizer.warehouse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "rack_rows",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rack_rows_warehouse_code",
                        columnNames = {"warehouse_id", "code"}
                ),
                @UniqueConstraint(
                        name = "uk_rack_rows_aisle_side",
                        columnNames = {"aisle_id", "side"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RackRow {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_rack_row")
    @SequenceGenerator(
            name = "seq_rack_row",
            sequenceName = "seq_rack_row",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id", nullable = false)
    private Aisle aisle;

    @Column(nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String getWarehouseCode() {
        return warehouse != null ? warehouse.getCode() : null;
    }

    private String getAisleCode() {
        return aisle != null ? aisle.getCode() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof RackRow rackRow)) {
            return false;
        }

        return Objects.equals(getWarehouseCode(), rackRow.getWarehouseCode())
                && Objects.equals(code, rackRow.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWarehouseCode(), code);
    }

    @Override
    public String toString() {
        return "RackRow{" +
                "id=" + id +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", aisleCode='" + getAisleCode() + '\'' +
                ", code='" + code + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                ", version=" + version +
                '}';
    }
}