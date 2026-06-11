package com.nikitaopara.warehouseoptimizer.warehouse.controller;

import com.nikitaopara.warehouseoptimizer.warehouse.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OperatorWarehouseController.class)
@AutoConfigureMockMvc(addFilters = false)
class OperatorWarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseService warehouseService;

    @Test
    void exposesWarehouseListForOperatorInterface() throws Exception {
        when(warehouseService.getWarehouses()).thenReturn(List.of());

        mockMvc.perform(get("/operator/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
