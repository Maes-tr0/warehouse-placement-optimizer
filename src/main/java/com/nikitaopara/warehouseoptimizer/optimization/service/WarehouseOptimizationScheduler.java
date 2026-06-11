package com.nikitaopara.warehouseoptimizer.optimization.service;

import com.nikitaopara.warehouseoptimizer.cache.config.CacheProperties;
import com.nikitaopara.warehouseoptimizer.cache.lock.SchedulerLockService;
import com.nikitaopara.warehouseoptimizer.optimization.model.OptimizationAssessmentTrigger;
import com.nikitaopara.warehouseoptimizer.warehouse.model.WarehouseStatus;
import com.nikitaopara.warehouseoptimizer.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarehouseOptimizationScheduler {

    private static final Logger log = LoggerFactory.getLogger(WarehouseOptimizationScheduler.class);

    private final WarehouseRepository warehouseRepository;
    private final WarehouseOptimizationAssessmentService assessmentService;
    private final SchedulerLockService lockService;
    private final CacheProperties cacheProperties;

    @Scheduled(
            cron = "${app.optimization.analysis-cron:0 0 2 * * *}",
            zone = "${app.optimization.analysis-zone:Europe/Amsterdam}"
    )
    public void analyzeActiveWarehouses() {
        warehouseRepository.findByStatus(WarehouseStatus.ACTIVE).forEach(warehouse -> {
            String lockName = "optimization-assessment:" + warehouse.getId();
            boolean executed = lockService.executeWithLock(
                    lockName,
                    cacheProperties.getSchedulerLockAtMost(),
                    () -> analyzeWarehouse(warehouse.getId())
            );

            if (!executed) {
                log.debug("Optimization assessment lock is held for warehouse {}", warehouse.getId());
            }
        });
    }

    private void analyzeWarehouse(Long warehouseId) {
        try {
            assessmentService.analyzeWarehouse(
                    warehouseId,
                    OptimizationAssessmentTrigger.SCHEDULED
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Scheduled optimization assessment failed for warehouse {}",
                    warehouseId,
                    exception
            );
        }
    }
}
