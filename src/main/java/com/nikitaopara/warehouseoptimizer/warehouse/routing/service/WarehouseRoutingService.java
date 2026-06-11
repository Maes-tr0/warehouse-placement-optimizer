package com.nikitaopara.warehouseoptimizer.warehouse.routing.service;

import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import com.nikitaopara.warehouseoptimizer.cache.config.CacheNames;
import com.nikitaopara.warehouseoptimizer.putaway.placement.service.PlacementTimeEstimationService;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.StoragePlaceRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.dto.WarehouseRoutePointResponse;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.dto.WarehouseRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseRoutingService {

    private final WarehouseRepository warehouseRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final WarehouseRouteCalculator routeCalculator;
    private final PlacementTimeEstimationService timeEstimationService;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.WAREHOUSE_ROUTES,
            key = "#warehouseId + ':' + #storagePlaceCode"
    )
    public WarehouseRouteResponse getRoute(Long warehouseId, String storagePlaceCode) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse not found: " + warehouseId);
        }

        List<StoragePlace> storagePlaces = storagePlaceRepository
                .findByWarehouseIdOrderByDistanceFromEntryMmAsc(warehouseId);
        StoragePlace target = storagePlaces.stream()
                .filter(place -> place.getCode().equals(storagePlaceCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Storage place not found: " + storagePlaceCode
                ));
        var route = routeCalculator.calculateRoute(storagePlaces, target)
                .orElseThrow(() -> new IllegalStateException(
                        "Storage place is unreachable from the warehouse entry: " + storagePlaceCode
                ));

        return new WarehouseRouteResponse(
                warehouseId,
                storagePlaceCode,
                route.distanceMm(),
                timeEstimationService.estimateHorizontalTravelTimeSeconds(route.distanceMm()),
                route.nodes().stream().map(WarehouseRoutePointResponse::from).toList()
        );
    }
}
