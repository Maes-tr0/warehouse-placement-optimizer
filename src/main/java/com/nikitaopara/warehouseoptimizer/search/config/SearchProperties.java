package com.nikitaopara.warehouseoptimizer.search.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.search")
@Getter
@Setter
public class SearchProperties {

    private boolean enabled;
    private int maximumPageSize = 200;
}
