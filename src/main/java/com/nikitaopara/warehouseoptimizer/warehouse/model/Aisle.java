package com.nikitaopara.warehouseoptimizer.warehouse.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
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

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "width_mm", nullable = false)
    private Integer widthMm;

    @Column(name = "length_mm", nullable = false)
    private Integer lengthMm;

    @Column(name = "distance_from_entry_mm", nullable = false)
    private Integer distanceFromEntryMm;

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
                ", distanceFromEntryMm=" + distanceFromEntryMm +
                ", version=" + version +
                '}';
    }

    private String getWarehouseCode() {
        return warehouse != null ? warehouse.getCode() : null;
    }
}