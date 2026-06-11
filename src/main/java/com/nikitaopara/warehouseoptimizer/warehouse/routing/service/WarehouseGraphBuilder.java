package com.nikitaopara.warehouseoptimizer.warehouse.routing.service;

import com.nikitaopara.warehouseoptimizer.warehouse.model.Aisle;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRouteNode;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRouteNodeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class WarehouseGraphBuilder {

    static final String ENTRY_NODE_ID = "ENTRY";

    WarehouseGraph build(List<StoragePlace> storagePlaces) {
        WarehouseGraph graph = new WarehouseGraph();
        graph.addNode(new WarehouseRouteNode(
                ENTRY_NODE_ID,
                WarehouseRouteNodeType.ENTRY,
                "Warehouse entry",
                0,
                0
        ));

        Map<AisleKey, List<StoragePlace>> placesByAisle = groupRoutablePlaces(storagePlaces);
        List<AisleKey> aisles = placesByAisle.keySet().stream()
                .sorted(Comparator.comparingInt(AisleKey::entryXMm)
                        .thenComparing(AisleKey::code))
                .toList();

        connectMainCorridor(graph, aisles);
        aisles.forEach(aisle -> connectAisle(graph, aisle, placesByAisle.get(aisle)));

        return graph;
    }

    String storagePlaceNodeId(StoragePlace storagePlace) {
        return "PLACE:" + stableStoragePlaceKey(storagePlace);
    }

    private Map<AisleKey, List<StoragePlace>> groupRoutablePlaces(List<StoragePlace> storagePlaces) {
        Map<AisleKey, List<StoragePlace>> result = new LinkedHashMap<>();

        for (StoragePlace place : storagePlaces) {
            if (!isRoutable(place)) {
                continue;
            }

            Aisle aisle = place.getRackRow().getAisle();
            AisleKey key = new AisleKey(
                    stableAisleKey(aisle),
                    aisle.getCode(),
                    aisle.getEntryXMm(),
                    aisle.getEntryYMm()
            );
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(place);
        }

        return result;
    }

    private void connectMainCorridor(WarehouseGraph graph, List<AisleKey> aisles) {
        String previousNodeId = ENTRY_NODE_ID;
        int previousXMm = 0;
        int previousYMm = 0;

        for (AisleKey aisle : aisles) {
            String aisleNodeId = aisleNodeId(aisle);
            graph.addNode(new WarehouseRouteNode(
                    aisleNodeId,
                    WarehouseRouteNodeType.AISLE_ENTRY,
                    aisle.code(),
                    aisle.entryXMm(),
                    aisle.entryYMm()
            ));
            graph.addBidirectionalEdge(
                    previousNodeId,
                    aisleNodeId,
                    manhattanDistance(previousXMm, previousYMm, aisle.entryXMm(), aisle.entryYMm())
            );
            previousNodeId = aisleNodeId;
            previousXMm = aisle.entryXMm();
            previousYMm = aisle.entryYMm();
        }
    }

    private void connectAisle(
            WarehouseGraph graph,
            AisleKey aisle,
            List<StoragePlace> storagePlaces
    ) {
        Map<Integer, List<StoragePlace>> placesByAccessY = storagePlaces.stream()
                .collect(LinkedHashMap::new,
                        (map, place) -> map.computeIfAbsent(
                                place.getAccessYMm(),
                                ignored -> new ArrayList<>()
                        ).add(place),
                        Map::putAll);
        List<Integer> accessPoints = placesByAccessY.keySet().stream().sorted().toList();
        String previousNodeId = aisleNodeId(aisle);
        int previousYMm = aisle.entryYMm();

        for (Integer accessYMm : accessPoints) {
            String accessNodeId = accessNodeId(aisle, accessYMm);
            graph.addNode(new WarehouseRouteNode(
                    accessNodeId,
                    WarehouseRouteNodeType.AISLE_ACCESS,
                    aisle.code() + " @ " + accessYMm + "mm",
                    aisle.entryXMm(),
                    accessYMm
            ));
            graph.addBidirectionalEdge(
                    previousNodeId,
                    accessNodeId,
                    Math.abs(accessYMm - previousYMm)
            );

            for (StoragePlace place : placesByAccessY.get(accessYMm)) {
                String placeNodeId = storagePlaceNodeId(place);
                graph.addNode(new WarehouseRouteNode(
                        placeNodeId,
                        WarehouseRouteNodeType.STORAGE_PLACE,
                        place.getCode(),
                        place.getAccessXMm(),
                        place.getAccessYMm()
                ));
                graph.addBidirectionalEdge(accessNodeId, placeNodeId, 0);
            }

            previousNodeId = accessNodeId;
            previousYMm = accessYMm;
        }
    }

    private boolean isRoutable(StoragePlace place) {
        return place != null
                && place.getCode() != null
                && place.getAccessXMm() != null
                && place.getAccessYMm() != null
                && place.getRackRow() != null
                && place.getRackRow().getAisle() != null
                && place.getRackRow().getAisle().getCode() != null
                && place.getRackRow().getAisle().getEntryXMm() != null
                && place.getRackRow().getAisle().getEntryYMm() != null;
    }

    private String aisleNodeId(AisleKey aisle) {
        return "AISLE:" + aisle.stableKey();
    }

    private String accessNodeId(AisleKey aisle, int accessYMm) {
        return "ACCESS:" + aisle.stableKey() + ":" + accessYMm;
    }

    private String stableStoragePlaceKey(StoragePlace place) {
        return place.getId() != null ? place.getId().toString() : place.getCode();
    }

    private String stableAisleKey(Aisle aisle) {
        return aisle.getId() != null ? aisle.getId().toString() : aisle.getCode();
    }

    private int manhattanDistance(int firstX, int firstY, int secondX, int secondY) {
        return Math.abs(firstX - secondX) + Math.abs(firstY - secondY);
    }

    private record AisleKey(
            String stableKey,
            String code,
            int entryXMm,
            int entryYMm
    ) {
        private AisleKey {
            Objects.requireNonNull(stableKey);
            Objects.requireNonNull(code);
        }
    }
}
