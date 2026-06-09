package com.nikitaopara.warehouseoptimizer.putaway.placement.service;

import com.nikitaopara.warehouseoptimizer.warehouse.model.RackLevel;
import com.nikitaopara.warehouseoptimizer.warehouse.model.StoragePlace;
import org.springframework.stereotype.Service;

@Service
public class PlacementTimeEstimationService {

    private static final int HORIZONTAL_TRAVEL_SPEED_MM_PER_SECOND = 1500;

    private static final int REACH_TRUCK_LIFT_SPEED_MM_PER_SECOND = 450;
    private static final int REACH_TRUCK_LOWER_SPEED_MM_PER_SECOND = 550;

    private static final int BASE_APPROACH_SECONDS = 8;
    private static final int BASE_PALLET_HANDLING_SECONDS = 25;
    private static final int REACH_TRUCK_ALIGNMENT_SECONDS = 15;

    public Integer estimatePlacementTimeSeconds(StoragePlace storagePlace) {
        if (storagePlace == null) {
            throw new IllegalArgumentException("Storage place is required for time estimation");
        }

        int travelTimeSeconds = calculateTravelTimeSeconds(storagePlace);
        int handlingTimeSeconds = calculateHandlingTimeSeconds(storagePlace);

        return travelTimeSeconds + handlingTimeSeconds;
    }

    private int calculateTravelTimeSeconds(StoragePlace storagePlace) {
        Integer distanceFromEntryMm = storagePlace.getDistanceFromEntryMm();

        if (distanceFromEntryMm == null || distanceFromEntryMm <= 0) {
            return 0;
        }

        return (int) Math.ceil(
                (double) distanceFromEntryMm / HORIZONTAL_TRAVEL_SPEED_MM_PER_SECOND
        );
    }

    private int calculateHandlingTimeSeconds(StoragePlace storagePlace) {
        return BASE_APPROACH_SECONDS
                + BASE_PALLET_HANDLING_SECONDS
                + calculateVerticalHandlingTimeSeconds(storagePlace);
    }

    private int calculateVerticalHandlingTimeSeconds(StoragePlace storagePlace) {
        RackLevel rackLevel = storagePlace.getRackLevel();

        if (rackLevel == null) {
            return 0;
        }

        Integer levelNumber = rackLevel.getLevelNumber();
        Integer heightFromFloorMm = rackLevel.getHeightFromFloorMm();

        if (levelNumber == null || levelNumber <= 1) {
            return 0;
        }

        if (heightFromFloorMm == null || heightFromFloorMm <= 0) {
            return 0;
        }

        int liftTimeSeconds = (int) Math.ceil(
                (double) heightFromFloorMm / REACH_TRUCK_LIFT_SPEED_MM_PER_SECOND
        );

        int lowerTimeSeconds = (int) Math.ceil(
                (double) heightFromFloorMm / REACH_TRUCK_LOWER_SPEED_MM_PER_SECOND
        );

        return liftTimeSeconds
                + lowerTimeSeconds
                + REACH_TRUCK_ALIGNMENT_SECONDS;
    }
}