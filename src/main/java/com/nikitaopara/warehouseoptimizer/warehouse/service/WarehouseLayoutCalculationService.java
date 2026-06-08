package com.nikitaopara.warehouseoptimizer.warehouse.service;

import org.springframework.stereotype.Service;

@Service
public class WarehouseLayoutCalculationService {

    /**
     * Euro pallet base size.
     * 800 mm — ширина палети вздовж балки / прольоту.
     * 1200 mm — глибина палети в стелаж.
     */
    private static final int EURO_PALLET_WIDTH_MM = 800;
    private static final int EURO_PALLET_LENGTH_MM = 1200;

    /**
     * Rack construction defaults.
     */
    private static final int RACK_DEPTH_MM = 1100;

    /**
     * Зазор між палетами всередині одного прольоту.
     */
    private static final int PALLET_CLEARANCE_MM = 75;

    /**
     * Товщина/ширина вертикальної опори між прольотами.
     */
    private static final int UPRIGHT_PROFILE_WIDTH_MM = 100;

    /**
     * Відступ на початку/кінці aisle по довжині ряду.
     */
    private static final int ROW_START_CLEARANCE_MM = 500;
    private static final int ROW_END_CLEARANCE_MM = 500;

    /**
     * Зазор між двома back-to-back рядами.
     * Тобто коли стоїть RackRow + RackRow спинками один до одного.
     */
    private static final int BACK_TO_BACK_GAP_MM = 300;

    /**
     * Додатковий технічний зазор біля стелажа з боку головного коридору / проходу.
     */
    private static final int RACK_SIDE_CLEARANCE_MM = 200;

    public int calculateBeamLengthMm(Integer palletPlacesPerLevel) {
        validatePositive(palletPlacesPerLevel, "palletPlacesPerLevel");

        return palletPlacesPerLevel * EURO_PALLET_WIDTH_MM
                + (palletPlacesPerLevel + 1) * PALLET_CLEARANCE_MM;
    }

    public int calculateAisleLengthMm(
            Integer baysPerRackRow,
            Integer palletPlacesPerLevel
    ) {
        validatePositive(baysPerRackRow, "baysPerRackRow");
        validatePositive(palletPlacesPerLevel, "palletPlacesPerLevel");

        int beamLengthMm = calculateBeamLengthMm(palletPlacesPerLevel);

        return ROW_START_CLEARANCE_MM
                + baysPerRackRow * beamLengthMm
                + (baysPerRackRow + 1) * UPRIGHT_PROFILE_WIDTH_MM
                + ROW_END_CLEARANCE_MM;
    }

    /**
     * Координата X входу в aisle.
     *
     * Логіка MVP:
     * - стартова точка оператора: (0, 0)
     * - X — рух по головному коридору
     * - Y — рух всередину aisle
     *
     * Перед першим aisle є одинарний ряд біля стіни.
     * Далі між aisle повторюється блок:
     * aisle width + два back-to-back rack rows.
     */
    public int calculateAisleEntryXMm(
            Integer aisleSequenceNumber,
            Integer aisleWidthMm
    ) {
        validatePositive(aisleSequenceNumber, "aisleSequenceNumber");
        validatePositive(aisleWidthMm, "aisleWidthMm");

        int firstAisleEntryXMm = calculateFirstAisleEntryXMm(aisleWidthMm);
        int aislePitchMm = calculateAislePitchMm(aisleWidthMm);

        return firstAisleEntryXMm + (aisleSequenceNumber - 1) * aislePitchMm;
    }

    public int calculateAisleEntryYMm() {
        return 0;
    }

    public int calculateRackBayAccessXMm(Integer aisleEntryXMm) {
        validateNotNull(aisleEntryXMm, "aisleEntryXMm");

        return aisleEntryXMm;
    }

    /**
     * Координата Y доступу до rack bay.
     * Для RackBay беремо центр прольоту по довжині aisle.
     */
    public int calculateRackBayAccessYMm(
            Integer bayNumber,
            Integer palletPlacesPerLevel
    ) {
        validatePositive(bayNumber, "bayNumber");
        validatePositive(palletPlacesPerLevel, "palletPlacesPerLevel");

        int bayStartYMm = calculateRackBayStartYMm(bayNumber, palletPlacesPerLevel);
        int beamLengthMm = calculateBeamLengthMm(palletPlacesPerLevel);

        return bayStartYMm + beamLengthMm / 2;
    }

    public int calculateStoragePlaceAccessXMm(Integer aisleEntryXMm) {
        validateNotNull(aisleEntryXMm, "aisleEntryXMm");

        return aisleEntryXMm;
    }

    /**
     * Координата Y конкретного палетомісця.
     * Беремо центр палети всередині прольоту.
     */
    public int calculateStoragePlaceAccessYMm(
            Integer bayNumber,
            Integer positionNumber,
            Integer palletPlacesPerLevel
    ) {
        validatePositive(bayNumber, "bayNumber");
        validatePositive(positionNumber, "positionNumber");
        validatePositive(palletPlacesPerLevel, "palletPlacesPerLevel");

        int bayStartYMm = calculateRackBayStartYMm(
                bayNumber,
                palletPlacesPerLevel
        );

        int positionCenterOffsetMm =
                PALLET_CLEARANCE_MM
                        + EURO_PALLET_WIDTH_MM / 2
                        + (positionNumber - 1) * (EURO_PALLET_WIDTH_MM + PALLET_CLEARANCE_MM);

        return bayStartYMm + positionCenterOffsetMm;
    }

    public int calculateDistanceFromEntryMm(
            Integer accessXMm,
            Integer accessYMm
    ) {
        validateNotNull(accessXMm, "accessXMm");
        validateNotNull(accessYMm, "accessYMm");

        return Math.abs(accessXMm) + Math.abs(accessYMm);
    }

    public int calculateDistanceFromAisleStartMm(Integer accessYMm) {
        validateNotNull(accessYMm, "accessYMm");

        return Math.abs(accessYMm);
    }

    private int calculateRackBayStartYMm(
            Integer bayNumber,
            Integer palletPlacesPerLevel
    ) {
        int beamLengthMm = calculateBeamLengthMm(palletPlacesPerLevel);

        return ROW_START_CLEARANCE_MM
                + (bayNumber - 1) * (beamLengthMm + UPRIGHT_PROFILE_WIDTH_MM);
    }

    private int calculateFirstAisleEntryXMm(Integer aisleWidthMm) {
        int singleRackBlockWidthMm = calculateSingleRackBlockWidthMm();

        return singleRackBlockWidthMm + aisleWidthMm / 2;
    }

    private int calculateAislePitchMm(Integer aisleWidthMm) {
        int doubleRackBlockWidthMm = calculateDoubleRackBlockWidthMm();

        return aisleWidthMm + doubleRackBlockWidthMm;
    }

    private int calculateSingleRackBlockWidthMm() {
        return RACK_DEPTH_MM + RACK_SIDE_CLEARANCE_MM;
    }

    private int calculateDoubleRackBlockWidthMm() {
        return 2 * RACK_DEPTH_MM
                + BACK_TO_BACK_GAP_MM
                + 2 * RACK_SIDE_CLEARANCE_MM;
    }

    private void validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }

    private void validateNotNull(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}