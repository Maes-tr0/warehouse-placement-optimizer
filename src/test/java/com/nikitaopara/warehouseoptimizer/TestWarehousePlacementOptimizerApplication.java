package com.nikitaopara.warehouseoptimizer;

import org.springframework.boot.SpringApplication;

public class TestWarehousePlacementOptimizerApplication {

    static void main(String[] args) {
        SpringApplication.from(WarehousePlacementOptimizerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
