package com.nikitaopara.warehouseoptimizer.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.notifications")
@Getter
@Setter
public class EmailNotificationProperties {

    private boolean emailEnabled;
    private String from = "warehouse-optimizer@localhost";
    private List<String> recipients = new ArrayList<>();
}
