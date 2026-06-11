package com.nikitaopara.warehouseoptimizer.warehouse.routing.service;

import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WarehouseRouteCalculator {

    private final WarehouseGraphBuilder graphBuilder;
    private final DijkstraWarehouseRouter router;

    public Map<Long, Integer> calculateDistances(List<StoragePlace> storagePlaces) {
        WarehouseGraph graph = graphBuilder.build(storagePlaces);
        Map<String, Integer> graphDistances = router.calculateDistances(
                graph,
                WarehouseGraphBuilder.ENTRY_NODE_ID
        );
        Map<Long, Integer> distancesByPlaceId = new LinkedHashMap<>();

        for (StoragePlace place : storagePlaces) {
            if (place.getId() == null) {
                continue;
            }

            Integer routeDistance = graphDistances.get(graphBuilder.storagePlaceNodeId(place));
            distancesByPlaceId.put(place.getId(), usableDistance(routeDistance, place));
        }

        return distancesByPlaceId;
    }

    public Optional<WarehouseRoute> calculateRoute(
            List<StoragePlace> storagePlaces,
            StoragePlace target
    ) {
        WarehouseGraph graph = graphBuilder.build(storagePlaces);
        Optional<WarehouseRoute> graphRoute = router.findShortestRoute(
                graph,
                WarehouseGraphBuilder.ENTRY_NODE_ID,
                graphBuilder.storagePlaceNodeId(target)
        );

        if (graphRoute.isPresent()) {
            return graphRoute;
        }

        Integer fallbackDistance = target.getDistanceFromEntryMm();
        if (fallbackDistance == null || fallbackDistance < 0) {
            return Optional.empty();
        }

        return Optional.of(new WarehouseRoute(List.of(), fallbackDistance));
    }

    private int usableDistance(Integer routeDistance, StoragePlace place) {
        if (routeDistance != null && routeDistance != Integer.MAX_VALUE) {
            return routeDistance;
        }

        Integer fallbackDistance = place.getDistanceFromEntryMm();
        return fallbackDistance == null ? 0 : Math.max(fallbackDistance, 0);
    }
}
