package com.nikitaopara.warehouseoptimizer.warehouse.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.warehouse.dto.CreateWarehouseRequest;
import com.nikitaopara.warehouseoptimizer.warehouse.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseGenerationService {

    private final WarehouseLayoutCalculationService calculationService;

    public Warehouse generate(User actor, CreateWarehouseRequest request) {
        Warehouse warehouse = Warehouse.builder()
                .code(request.warehouseCode())
                .name(request.warehouseName())
                .layoutType(resolveLayoutType(request))
                .status(WarehouseStatus.ACTIVE)
                .createdBy(actor)
                .build();

        generateMainCorridorOneSideAislesLayout(warehouse, request);

        return warehouse;
    }

    private WarehouseLayoutType resolveLayoutType(CreateWarehouseRequest request) {
        if (request.layoutType() == null) {
            return WarehouseLayoutType.MAIN_CORRIDOR_ONE_SIDE_AISLES;
        }

        return request.layoutType();
    }

    private void generateMainCorridorOneSideAislesLayout(
            Warehouse warehouse,
            CreateWarehouseRequest request
    ) {
        int createdRackRows = 0;

        for (int aisleIndex = 1; aisleIndex <= request.aisleCount(); aisleIndex++) {
            int aisleEntryXMm = calculationService.calculateAisleEntryXMm(
                    aisleIndex,
                    request.aisleWidthMm()
            );

            int aisleEntryYMm = calculationService.calculateAisleEntryYMm();

            int aisleDistanceFromEntryMm = calculationService.calculateDistanceFromEntryMm(
                    aisleEntryXMm,
                    aisleEntryYMm
            );

            int aisleLengthMm = calculationService.calculateAisleLengthMm(
                    request.baysPerRackRow(),
                    request.palletPlacesPerLevel()
            );

            Aisle aisle = Aisle.builder()
                    .code(generateAisleCode(aisleIndex))
                    .sequenceNumber(aisleIndex)
                    .widthMm(request.aisleWidthMm())
                    .lengthMm(aisleLengthMm)
                    .entryXMm(aisleEntryXMm)
                    .entryYMm(aisleEntryYMm)
                    .distanceFromEntryMm(aisleDistanceFromEntryMm)
                    .build();

            warehouse.addAisle(aisle);

            int rackRowsForAisle = Math.min(
                    2,
                    request.rackRowCount() - createdRackRows
            );

            for (int rackRowIndexInAisle = 1; rackRowIndexInAisle <= rackRowsForAisle; rackRowIndexInAisle++) {
                createdRackRows++;

                RackRow rackRow = RackRow.builder()
                        .code(generateRackRowCode(createdRackRows))
                        .sequenceNumber(createdRackRows)
                        .build();

                aisle.addRackRow(rackRow);

                generateRackBays(rackRow, request);
            }
        }
    }

    private void generateRackBays(
            RackRow rackRow,
            CreateWarehouseRequest request
    ) {
        Aisle aisle = rackRow.getAisle();

        for (int bayNumber = 1; bayNumber <= request.baysPerRackRow(); bayNumber++) {
            int beamLengthMm = calculationService.calculateBeamLengthMm(
                    request.palletPlacesPerLevel()
            );

            int accessXMm = calculationService.calculateRackBayAccessXMm(
                    aisle.getEntryXMm()
            );

            int distanceFromAisleStartMm = calculationService.calculateRackBayAccessYMm(
                    bayNumber,
                    request.palletPlacesPerLevel()
            );

            RackBay rackBay = RackBay.builder()
                    .code(rackRow.getCode() + "-" + String.format("%02d", bayNumber))
                    .bayNumber(bayNumber)
                    .positionsPerLevel(request.palletPlacesPerLevel())
                    .beamLengthMm(beamLengthMm)
                    .maxBayLoadKg(request.maxBayLoadKg())
                    .accessXMm(accessXMm)
                    .accessYMm(distanceFromAisleStartMm)
                    .distanceFromAisleStartMm(distanceFromAisleStartMm)
                    .build();

            rackRow.addRackBay(rackBay);

            generateRackLevels(rackBay, request);
        }
    }

    private void generateRackLevels(
            RackBay rackBay,
            CreateWarehouseRequest request
    ) {
        int heightFromFloorMm = 0;

        for (var profile : request.levelProfiles()) {
            RackLevel rackLevel = RackLevel.builder()
                    .code(rackBay.getCode() + "-L" + profile.levelNumber())
                    .levelNumber(profile.levelNumber())
                    .clearHeightMm(profile.clearHeightMm())
                    .heightFromFloorMm(heightFromFloorMm)
                    .maxLevelLoadKg(profile.maxCellLoadKg() * request.palletPlacesPerLevel())
                    .build();

            rackBay.addRackLevel(rackLevel);

            generateStoragePlaces(
                    rackLevel,
                    profile.maxCellLoadKg(),
                    profile.clearHeightMm(),
                    request
            );

            heightFromFloorMm += profile.clearHeightMm();
        }
    }

    private void generateStoragePlaces(
            RackLevel rackLevel,
            Integer maxCellLoadKg,
            Integer clearHeightMm,
            CreateWarehouseRequest request
    ) {
        RackBay rackBay = rackLevel.getRackBay();
        RackRow rackRow = rackBay.getRackRow();
        Aisle aisle = rackRow.getAisle();

        for (int positionNumber = 1; positionNumber <= request.palletPlacesPerLevel(); positionNumber++) {
            int positionIndexInRow =
                    (rackBay.getBayNumber() - 1) * request.palletPlacesPerLevel()
                            + (positionNumber - 1);

            int accessXMm = calculationService.calculateStoragePlaceAccessXMm(
                    aisle.getEntryXMm()
            );

            int distanceFromAisleStartMm = calculationService.calculateStoragePlaceAccessYMm(
                    rackBay.getBayNumber(),
                    positionNumber,
                    request.palletPlacesPerLevel()
            );

            int distanceFromEntryMm = calculationService.calculateDistanceFromEntryMm(
                    accessXMm,
                    distanceFromAisleStartMm
            );

            StoragePlace storagePlace = StoragePlace.builder()
                    .code(generateStoragePlaceCode(
                            rackRow.getCode(),
                            rackLevel.getLevelNumber(),
                            positionIndexInRow
                    ))
                    .positionNumber(positionNumber)
                    .maxWeightKg(maxCellLoadKg)
                    .maxHeightMm(clearHeightMm)
                    .accessXMm(accessXMm)
                    .accessYMm(distanceFromAisleStartMm)
                    .distanceFromAisleStartMm(distanceFromAisleStartMm)
                    .distanceFromEntryMm(distanceFromEntryMm)
                    .status(StoragePlaceStatus.AVAILABLE)
                    .build();

            rackLevel.addStoragePlace(storagePlace);
        }
    }

    private String generateAisleCode(int sequenceNumber) {
        return "A" + String.format("%02d", sequenceNumber);
    }

    private String generateRackRowCode(int sequenceNumber) {
        int zeroBased = sequenceNumber - 1;

        char first = (char) ('A' + (zeroBased / 26));
        char second = (char) ('A' + (zeroBased % 26));

        return "" + first + second;
    }

    private String generateStoragePlaceCode(
            String rackRowCode,
            int levelNumber,
            int positionIndexInRow
    ) {
        return rackRowCode + levelNumber + String.format("%02d", positionIndexInRow);
    }
}