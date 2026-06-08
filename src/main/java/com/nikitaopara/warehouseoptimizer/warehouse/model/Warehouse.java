package com.nikitaopara.warehouseoptimizer.warehouse.model;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "warehouses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_warehouses_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_warehouse")
    @SequenceGenerator(
            name = "seq_warehouse",
            sequenceName = "seq_warehouse",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, updatable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "layout_type", nullable = false, length = 80)
    private WarehouseLayoutType layoutType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WarehouseStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @OneToMany(
            mappedBy = "warehouse",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Aisle> aisles = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addAisle(Aisle aisle) {
        if (aisle == null) {
            return;
        }

        this.aisles.add(aisle);
        aisle.setWarehouse(this);
    }

    public void removeAisle(Aisle aisle) {
        if (aisle == null) {
            return;
        }

        this.aisles.remove(aisle);
        aisle.setWarehouse(null);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = WarehouseStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Warehouse warehouse)) {
            return false;
        }

        return code != null && Objects.equals(code, warehouse.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Warehouse{" +
                "id=" + id +
                ", warehouseCode='" + code + '\'' +
                ", warehouseName='" + name + '\'' +
                ", layoutType=" + layoutType +
                ", status=" + status +
                ", version=" + version +
                ", createdBy=" + (createdBy != null ? createdBy.getFullName() : null) +
                ", aisleCount=" + (aisles != null ? aisles.size() : 0) +
                '}';
    }
}