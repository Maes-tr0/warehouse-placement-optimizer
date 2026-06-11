package com.nikitaopara.warehouseoptimizer.eventing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.events")
@Getter
@Setter
public class EventingProperties {

    private boolean kafkaEnabled;
    private int dispatchBatchSize = 50;
    private Duration sendTimeout = Duration.ofSeconds(10);
    private Duration processingLease = Duration.ofMinutes(1);
    private Duration initialRetryDelay = Duration.ofSeconds(10);
    private Duration maximumRetryDelay = Duration.ofMinutes(15);
}
