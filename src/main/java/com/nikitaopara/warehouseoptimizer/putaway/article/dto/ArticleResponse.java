package com.nikitaopara.warehouseoptimizer.putaway.article.dto;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import com.nikitaopara.warehouseoptimizer.putaway.article.model.UnitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ArticleResponse(
        Long id,
        String articleNumber,
        String name,
        UnitType unitType,
        Integer unitWidthMm,
        Integer unitLengthMm,
        Integer unitHeightMm,
        BigDecimal unitWeightKg,
        Integer maxQuantityPerPallet,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getArticleNumber(),
                article.getName(),
                article.getUnitType(),
                article.getUnitWidthMm(),
                article.getUnitLengthMm(),
                article.getUnitHeightMm(),
                article.getUnitWeightKg(),
                article.getMaxQuantityPerPallet(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}