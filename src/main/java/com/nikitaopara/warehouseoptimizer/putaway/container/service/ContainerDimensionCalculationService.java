package com.nikitaopara.warehouseoptimizer.putaway.container.service;

import com.nikitaopara.warehouseoptimizer.putaway.article.model.Article;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ContainerDimensionCalculationService {

    private static final int EURO_PALLET_WIDTH_MM = 800;
    private static final int EURO_PALLET_LENGTH_MM = 1200;
    private static final int EURO_PALLET_BASE_HEIGHT_MM = 144;

    public BigDecimal calculateWeightKg(Article article, Integer quantity) {
        if (article == null || article.getUnitWeightKg() == null || quantity == null) {
            throw new IllegalArgumentException("Article and quantity are required for weight calculation");
        }

        return article.getUnitWeightKg()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(3, RoundingMode.HALF_UP);
    }

    public Integer calculateHeightMm(Article article, Integer quantity) {
        if (article == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Article and positive quantity are required for height calculation");
        }

        int unitsPerLayer = calculateUnitsPerLayer(article);

        int layerCount = (int) Math.ceil((double) quantity / unitsPerLayer);

        return EURO_PALLET_BASE_HEIGHT_MM + layerCount * article.getUnitHeightMm();
    }

    private int calculateUnitsPerLayer(Article article) {
        if (article.getUnitWidthMm() == null
                || article.getUnitLengthMm() == null
                || article.getUnitWidthMm() <= 0
                || article.getUnitLengthMm() <= 0) {
            throw new IllegalArgumentException("Article unit width and length are required for layer calculation");
        }

        int byDefaultOrientation =
                (EURO_PALLET_WIDTH_MM / article.getUnitWidthMm())
                        * (EURO_PALLET_LENGTH_MM / article.getUnitLengthMm());

        int byRotatedOrientation =
                (EURO_PALLET_WIDTH_MM / article.getUnitLengthMm())
                        * (EURO_PALLET_LENGTH_MM / article.getUnitWidthMm());

        int unitsPerLayer = Math.max(byDefaultOrientation, byRotatedOrientation);

        if (unitsPerLayer <= 0) {
            throw new IllegalArgumentException("Article unit dimensions do not fit euro pallet");
        }

        return unitsPerLayer;
    }
}