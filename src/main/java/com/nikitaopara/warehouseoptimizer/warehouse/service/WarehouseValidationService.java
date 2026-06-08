package com.nikitaopara.warehouseoptimizer.warehouse.service;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateRackLevelProfileRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseLayoutType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WarehouseValidationService {

    private static final int MIN_PALLET_PLACES_PER_LEVEL = 1;
    private static final int MAX_PALLET_PLACES_PER_LEVEL = 4;

    private final WarehouseDataService warehouseDataService;

    public void validateCreateWarehouseRequest(User actor, CreateWarehouseRequest request) {
        validateActor(actor);
        validateRequestExists(request);
        validateBasicFields(request);
        validateWarehouseCodeIsFree(request.warehouseCode());
        validateLayoutType(request);
        validateLayoutFields(request);
        validateLevelProfiles(request);
        validateBayLoad(request);
    }

    private void validateActor(User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() != Role.ROOT_ADMIN) {
            throw new AccessDeniedException("Only ROOT_ADMIN can create warehouse");
        }
    }

    private void validateRequestExists(CreateWarehouseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create warehouse request cannot be null");
        }
    }

    private void validateBasicFields(CreateWarehouseRequest request) {
        if (!StringUtils.hasText(request.warehouseCode())) {
            throw new IllegalArgumentException("Warehouse code is required");
        }

        if (!StringUtils.hasText(request.warehouseName())) {
            throw new IllegalArgumentException("Warehouse name is required");
        }
    }

    private void validateWarehouseCodeIsFree(String warehouseCode) {
        if (warehouseDataService.existsByCode(warehouseCode)) {
            throw new IllegalArgumentException("Warehouse with this code already exists");
        }
    }

    private void validateLayoutType(CreateWarehouseRequest request) {
        if (request.layoutType() == null) {
            throw new IllegalArgumentException("Warehouse layout type is required");
        }

        if (request.layoutType() != WarehouseLayoutType.MAIN_CORRIDOR_ONE_SIDE_AISLES) {
            throw new IllegalArgumentException(
                    "Unsupported warehouse layout type: " + request.layoutType()
            );
        }
    }

    private void validateLayoutFields(CreateWarehouseRequest request) {
        if (request.aisleCount() == null || request.aisleCount() <= 0) {
            throw new IllegalArgumentException("Aisle count must be greater than zero");
        }

        if (request.rackRowCount() == null || request.rackRowCount() <= 0) {
            throw new IllegalArgumentException("Rack row count must be greater than zero");
        }

        if (request.rackRowCount() < request.aisleCount()) {
            throw new IllegalArgumentException("Rack row count cannot be less than aisle count");
        }

        if (request.rackRowCount() > request.aisleCount() * 2) {
            throw new IllegalArgumentException("For this layout, one aisle can have only one or two rack rows");
        }

        if (request.baysPerRackRow() == null || request.baysPerRackRow() <= 0) {
            throw new IllegalArgumentException("Bays per rack row must be greater than zero");
        }

        if (request.palletPlacesPerLevel() == null) {
            throw new IllegalArgumentException("Pallet places per level is required");
        }

        if (request.palletPlacesPerLevel() < MIN_PALLET_PLACES_PER_LEVEL
                || request.palletPlacesPerLevel() > MAX_PALLET_PLACES_PER_LEVEL) {
            throw new IllegalArgumentException("Pallet places per level must be between 1 and 4");
        }

        if (request.aisleWidthMm() == null || request.aisleWidthMm() <= 0) {
            throw new IllegalArgumentException("Aisle width must be greater than zero");
        }

        if (request.maxBayLoadKg() == null || request.maxBayLoadKg() <= 0) {
            throw new IllegalArgumentException("Max bay load must be greater than zero");
        }
    }

    private void validateLevelProfiles(CreateWarehouseRequest request) {
        if (request.levelProfiles() == null || request.levelProfiles().isEmpty()) {
            throw new IllegalArgumentException("Level profiles are required");
        }

        Set<Integer> levelNumbers = new HashSet<>();

        for (CreateRackLevelProfileRequest profile : request.levelProfiles()) {
            if (profile == null) {
                throw new IllegalArgumentException("Level profile cannot be null");
            }

            if (profile.levelNumber() == null || profile.levelNumber() <= 0) {
                throw new IllegalArgumentException("Level number must be greater than zero");
            }

            if (!levelNumbers.add(profile.levelNumber())) {
                throw new IllegalArgumentException("Duplicate level number: " + profile.levelNumber());
            }

            if (profile.clearHeightMm() == null || profile.clearHeightMm() <= 0) {
                throw new IllegalArgumentException("Clear height must be greater than zero");
            }

            if (profile.maxCellLoadKg() == null || profile.maxCellLoadKg() <= 0) {
                throw new IllegalArgumentException("Max cell load must be greater than zero");
            }
        }
    }

    private void validateBayLoad(CreateWarehouseRequest request) {
        long totalBayLoadKg = request.levelProfiles()
                .stream()
                .mapToLong(profile -> (long) profile.maxCellLoadKg() * request.palletPlacesPerLevel())
                .sum();

        if (totalBayLoadKg > request.maxBayLoadKg()) {
            throw new IllegalArgumentException(
                    "Total calculated bay load " + totalBayLoadKg +
                            " kg exceeds max bay load " + request.maxBayLoadKg() + " kg"
            );
        }
    }
}