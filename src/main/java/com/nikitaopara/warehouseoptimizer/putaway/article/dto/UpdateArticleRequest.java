package com.nikitaopara.warehouseoptimizer.putaway.article.dto;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.UnitType;

import java.math.BigDecimal;

public record UpdateArticleRequest(
        String name,
        UnitType unitType,
        Integer unitWidthMm,
        Integer unitLengthMm,
        Integer unitHeightMm,
        BigDecimal unitWeightKg,
        Integer maxQuantityPerPallet
) {
}