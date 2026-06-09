package com.nikitaopara.warehouseoptimizer.demand.model;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "order_demand_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_demand_items_order_article",
                        columnNames = {"order_demand_id", "article_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_order_demand_items_article",
                        columnList = "article_id"
                ),
                @Index(
                        name = "idx_order_demand_items_warehouse_article",
                        columnList = "warehouse_id, article_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDemandItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_order_demand_item")
    @SequenceGenerator(
            name = "seq_order_demand_item",
            sequenceName = "seq_order_demand_item",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_demand_id", nullable = false)
    private OrderDemand orderDemand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false)
    private Integer quantity;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void setOrderDemand(OrderDemand orderDemand) {
        this.orderDemand = orderDemand;

        if (orderDemand != null) {
            this.warehouse = orderDemand.getWarehouse();
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

    private String getOrderNumber() {
        return orderDemand != null ? orderDemand.getOrderNumber() : null;
    }

    private String getArticleNumber() {
        return article != null ? article.getArticleNumber() : null;
    }

    private String getWarehouseCode() {
        return warehouse != null ? warehouse.getCode() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof OrderDemandItem that)) {
            return false;
        }

        return Objects.equals(getWarehouseCode(), that.getWarehouseCode())
                && Objects.equals(getOrderNumber(), that.getOrderNumber())
                && Objects.equals(getArticleNumber(), that.getArticleNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWarehouseCode(), getOrderNumber(), getArticleNumber());
    }

    @Override
    public String toString() {
        return "OrderDemandItem{" +
                "id=" + id +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", orderNumber='" + getOrderNumber() + '\'' +
                ", articleNumber='" + getArticleNumber() + '\'' +
                ", quantity=" + quantity +
                ", version=" + version +
                '}';
    }
}