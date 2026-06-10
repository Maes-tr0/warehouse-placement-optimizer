package com.nikitaopara.warehouseoptimizer.putaway.container.model;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "containers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_containers_container_number", columnNames = "container_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_container")
    @SequenceGenerator(
            name = "seq_container",
            sequenceName = "seq_container",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "container_number", nullable = false, unique = true, updatable = false, length = 100)
    private String containerNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "height_mm", nullable = false)
    private Integer heightMm;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_storage_place_id", unique = true)
    private StoragePlace currentStoragePlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ContainerStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_container_id")
    private Container mergedIntoContainer;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

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

        if (this.receivedAt == null) {
            this.receivedAt = now;
        }

        if (this.status == null) {
            this.status = ContainerStatus.WAITING_FOR_PLACEMENT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isStored() {
        return this.status == ContainerStatus.STORED;
    }

    public boolean isWaitingForPlacement() {
        return this.status == ContainerStatus.WAITING_FOR_PLACEMENT;
    }

    public boolean isMerged() {
        return this.status == ContainerStatus.MERGED;
    }

    public boolean isRemoved() {
        return this.status == ContainerStatus.REMOVED;
    }

    public boolean isFull() {
        if (article == null || article.getMaxQuantityPerPallet() == null || quantity == null) {
            return false;
        }

        return quantity >= article.getMaxQuantityPerPallet();
    }

    public boolean canAcceptQuantity(Integer incomingQuantity) {
        if (article == null || quantity == null || incomingQuantity == null) {
            return false;
        }

        int totalQuantity = quantity + incomingQuantity;

        return article.canFitQuantity(totalQuantity);
    }

    public Integer getRemainingQuantityCapacity() {
        if (article == null || quantity == null) {
            return 0;
        }

        return article.calculateRemainingQuantityCapacity(quantity);
    }

    public void markAsStored(StoragePlace storagePlace) {
        this.currentStoragePlace = storagePlace;
        this.status = ContainerStatus.STORED;
    }

    public void relocateTo(StoragePlace storagePlace) {
        if (!isStored()) {
            throw new IllegalStateException("Only stored container can be relocated");
        }

        this.currentStoragePlace = storagePlace;
    }

    public void markAsMergedInto(Container targetContainer) {
        this.currentStoragePlace = null;
        this.mergedIntoContainer = targetContainer;
        this.status = ContainerStatus.MERGED;
    }

    public void markAsRemoved() {
        this.currentStoragePlace = null;
        this.status = ContainerStatus.REMOVED;
    }

    private String getArticleNumber() {
        return article != null ? article.getArticleNumber() : null;
    }

    private String getStoragePlaceCode() {
        return currentStoragePlace != null ? currentStoragePlace.getCode() : null;
    }

    private String getMergedIntoContainerNumber() {
        return mergedIntoContainer != null ? mergedIntoContainer.getContainerNumber() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Container container)) {
            return false;
        }

        return containerNumber != null && Objects.equals(containerNumber, container.containerNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerNumber);
    }

    @Override
    public String toString() {
        return "Container{" +
                "id=" + id +
                ", warehouseCode='" + getWarehouseCode() + '\'' +
                ", containerNumber='" + containerNumber + '\'' +
                ", articleNumber='" + getArticleNumber() + '\'' +
                ", quantity=" + quantity +
                ", weightKg=" + weightKg +
                ", heightMm=" + heightMm +
                ", currentStoragePlaceCode='" + getStoragePlaceCode() + '\'' +
                ", status=" + status +
                ", mergedIntoContainerNumber='" + getMergedIntoContainerNumber() + '\'' +
                ", receivedAt=" + receivedAt +
                ", version=" + version +
                '}';
    }

    private String getWarehouseCode() {
        return warehouse != null ? warehouse.getCode() : null;
    }
}
