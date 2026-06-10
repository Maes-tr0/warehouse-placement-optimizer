package com.nikitaopara.warehouseoptimizer.optimization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.optimization")
@Getter
@Setter
public class OptimizationProperties {

    private BigDecimal thresholdPercent = new BigDecimal("60.00");
    private int lookbackDays = 730;
    private int recencyHalfLifeDays = 365;
    private int seasonalWindowDays = 45;
    private int minimumDemandObservations = 30;
    private BigDecimal targetPercent = new BigDecimal("85.00");
    private int maximumPlanSteps = 30;
    private long minimumTimeSavingSeconds = 1;
}
