package com.nikitaopara.warehouseoptimizer.putaway.article.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "articles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_articles_article_number", columnNames = "article_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_article")
    @SequenceGenerator(
            name = "seq_article",
            sequenceName = "seq_article",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "article_number", nullable = false, unique = true, updatable = false, length = 50)
    private String articleNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 50)
    private UnitType unitType;

    @Column(name = "unit_width_mm", nullable = false)
    private Integer unitWidthMm;

    @Column(name = "unit_length_mm", nullable = false)
    private Integer unitLengthMm;

    @Column(name = "unit_height_mm", nullable = false)
    private Integer unitHeightMm;

    @Column(name = "unit_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal unitWeightKg;

    @Column(name = "max_quantity_per_pallet", nullable = false)
    private Integer maxQuantityPerPallet;

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

    public boolean canFitQuantity(Integer quantity) {
        if (quantity == null || maxQuantityPerPallet == null) {
            return false;
        }

        return quantity <= maxQuantityPerPallet;
    }

    public Integer calculateRemainingQuantityCapacity(Integer currentQuantity) {
        if (currentQuantity == null || maxQuantityPerPallet == null) {
            return 0;
        }

        return Math.max(maxQuantityPerPallet - currentQuantity, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Article article)) {
            return false;
        }

        return articleNumber != null && Objects.equals(articleNumber, article.articleNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleNumber);
    }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", articleNumber='" + articleNumber + '\'' +
                ", name='" + name + '\'' +
                ", unitType=" + unitType +
                ", unitWidthMm=" + unitWidthMm +
                ", unitLengthMm=" + unitLengthMm +
                ", unitHeightMm=" + unitHeightMm +
                ", unitWeightKg=" + unitWeightKg +
                ", maxQuantityPerPallet=" + maxQuantityPerPallet +
                ", version=" + version +
                '}';
    }
}