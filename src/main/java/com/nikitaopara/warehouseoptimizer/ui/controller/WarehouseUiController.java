package com.nikitaopara.warehouseoptimizer.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WarehouseUiController {

    @GetMapping({"/", "/index.html"})
    public String index() {
        return "index";
    }
}
