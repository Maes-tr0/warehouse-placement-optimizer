package com.nikitaopara.warehouseoptimizer.warehouse.routing.service;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Aisle;
import com.nikitaopara.warehouseoptimizer.warehouse.model.RackRow;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseRouteCalculatorTest {

    private final WarehouseRouteCalculator calculator = new WarehouseRouteCalculator(
            new WarehouseGraphBuilder(),
            new DijkstraWarehouseRouter()
    );

    @Test
    void calculatesRouteThroughMainCorridorAndAisle() {
        Aisle firstAisle = aisle(1L, "A01", 3_000);
        Aisle secondAisle = aisle(2L, "A02", 8_000);
        StoragePlace firstPlace = place(10L, "AA100", firstAisle, 2_000);
        StoragePlace secondPlace = place(11L, "AB100", secondAisle, 4_000);

        var distances = calculator.calculateDistances(List.of(firstPlace, secondPlace));
        var route = calculator.calculateRoute(
                List.of(firstPlace, secondPlace),
                secondPlace
        ).orElseThrow();

        assertThat(distances).containsEntry(firstPlace.getId(), 5_000);
        assertThat(distances).containsEntry(secondPlace.getId(), 12_000);
        assertThat(route.distanceMm()).isEqualTo(12_000);
        assertThat(route.nodes()).extracting(node -> node.label())
                .containsExactly(
                        "Warehouse entry",
                        "A01",
                        "A02",
                        "A02 @ 4000mm",
                        "AB100"
                );
    }

    @Test
    void fallsBackToStoredDistanceWhenTopologyIsIncomplete() {
        StoragePlace legacyPlace = StoragePlace.builder()
                .id(12L)
                .code("LEGACY")
                .distanceFromEntryMm(7_500)
                .build();

        assertThat(calculator.calculateDistances(List.of(legacyPlace)))
                .containsEntry(legacyPlace.getId(), 7_500);
        assertThat(calculator.calculateRoute(List.of(legacyPlace), legacyPlace).orElseThrow()
                .distanceMm()).isEqualTo(7_500);
    }

    private Aisle aisle(Long id, String code, int entryXMm) {
        return Aisle.builder()
                .id(id)
                .code(code)
                .entryXMm(entryXMm)
                .entryYMm(0)
                .build();
    }

    private StoragePlace place(Long id, String code, Aisle aisle, int accessYMm) {
        RackRow row = RackRow.builder()
                .id(id)
                .code("ROW-" + id)
                .aisle(aisle)
                .build();

        return StoragePlace.builder()
                .id(id)
                .code(code)
                .rackRow(row)
                .accessXMm(aisle.getEntryXMm())
                .accessYMm(accessYMm)
                .distanceFromEntryMm(aisle.getEntryXMm() + accessYMm)
                .build();
    }
}
