package com.nikitaopara.warehouseoptimizer.demand.model;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "order_demands",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_demands_warehouse_order_number",
                        columnNames = {"warehouse_id", "order_number"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_order_demands_warehouse_date",
                        columnList = "warehouse_id, order_date_time"
                ),
                @Index(
                        name = "idx_order_demands_order_number",
                        columnList = "order_number"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_order_demand")
    @SequenceGenerator(
            name = "seq_order_demand",
            sequenceName = "seq_order_demand",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @OneToMany(
            mappedBy = "orderDemand",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<OrderDemandItem> items = new ArrayList<>();

    @Column(name = "order_number", nullable = false, length = 100)
    private String orderNumber;

    @Column(name = "order_date_time", nullable = false)
    private LocalDateTime orderDateTime;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addItem(OrderDemandItem item) {
        if (item == null) {
            return;
        }

        this.items.add(item);
        item.setOrderDemand(this);
        item.setWarehouse(this.warehouse);
    }

    public void removeItem(OrderDemandItem item) {
        if (item == null) {
            return;
        }

        this.items.remove(item);
        item.setOrderDemand(null);
        item.setWarehouse(null);
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;

        if (items != null) {
            items.forEach(item -> item.setWarehouse(warehouse));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof OrderDemand that)) {
            return false;
        }

        return Objects.equals(getWarehouseCode(), that.getWarehouseCode())
                && Objects.equals(orderNumber, that.orderNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWarehouseCode(), orderNumber);
    }

    @Override
    public String toString() {
        return "OrderDemand{" +
                "id=" + id +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", orderNumber='" + orderNumber + '\'' +
                ", orderDateTime=" + orderDateTime +
                ", itemCount=" + (items != null ? items.size() : 0) +
                ", version=" + version +
                '}';
    }
}