package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.putaway.container.model.Container;
import com.nikitaopara.warehouseoptimizer.putaway.placement.model.PlacementScoreResult;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.model.Warehouse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementRecommendationFactoryTest {

    @Test
    void createsRecommendationWithConfiguredExpiration() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-06-10T10:00:00Z"),
                ZoneOffset.UTC
        );
        PlacementRecommendationFactory factory = new PlacementRecommendationFactory(clock, 15);

        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-1").build();
        Container sourceContainer = Container.builder()
                .id(10L)
                .containerNumber("CONT-1")
                .warehouse(warehouse)
                .build();
        StoragePlace storagePlace = StoragePlace.builder().id(20L).code("AA100").build();
        PlacementScoreResult score = new PlacementScoreResult(
                1_000,
                40,
                BigDecimal.valueOf(999_960),
                "Place container"
        );

        var recommendation = factory.createPlaceRecommendation(
                sourceContainer,
                storagePlace,
                score
        );

        assertThat(recommendation.getExpiresAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 10, 10, 15));
    }
}
