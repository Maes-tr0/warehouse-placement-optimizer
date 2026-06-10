package com.nikitaopara.warehouseoptimizer.demand.forecast.service;

import com.nikitaopara.warehouseoptimizer.demand.forecast.config.DemandForecastProperties;
import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class DemandForecastTrainingScheduler {

    private static final Logger log = LoggerFactory.getLogger(DemandForecastTrainingScheduler.class);

    private final WarehouseRepository warehouseRepository;
    private final DemandForecastRetrainingService retrainingService;
    private final DemandForecastProperties properties;

    @Scheduled(
            cron = "${app.demand-forecast.training-cron:0 30 3 * * *}",
            zone = "${app.demand-forecast.training-zone:Europe/Amsterdam}"
    )
    public void trainActiveWarehousesIfNeeded() {
        LocalDate today = LocalDate.now(ZoneId.of(properties.getTrainingZone()));

        warehouseRepository.findByStatus(WarehouseStatus.ACTIVE).forEach(warehouse -> {
            try {
                retrainingService.trainIfNeeded(warehouse.getId(), today);
            } catch (IllegalArgumentException exception) {
                log.info(
                        "Scheduled demand forecast training skipped for warehouse {}: {}",
                        warehouse.getId(),
                        exception.getMessage()
                );
            } catch (RuntimeException exception) {
                log.error(
                        "Scheduled demand forecast training failed for warehouse {}",
                        warehouse.getId(),
                        exception
                );
            }
        });
    }
}
