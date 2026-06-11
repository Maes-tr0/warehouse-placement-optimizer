package com.nikitaopara.warehouseoptimizer.ui.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = WarehouseUiController.class)
@AutoConfigureMockMvc(addFilters = false)
class WarehouseUiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void redirectsAdministratorToAdminDashboard() throws Exception {
        mockMvc.perform(get("/").principal(authentication("ROLE_ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/app/admin/dashboard"));
    }

    @Test
    void redirectsOperatorToOperatorDashboard() throws Exception {
        mockMvc.perform(get("/").principal(authentication("ROLE_OPERATOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/app/operator/dashboard"));
    }

    @ParameterizedTest
    @CsvSource({
            "/app/admin/dashboard, admin/dashboard",
            "/app/admin/warehouse, admin/warehouse",
            "/app/admin/inventory, admin/inventory",
            "/app/admin/demand, admin/demand",
            "/app/admin/optimization, admin/optimization",
            "/app/admin/accounts, admin/accounts"
    })
    @WithMockUser(roles = "ADMIN")
    void rendersAdminPages(String path, String viewName) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName));
    }

    @ParameterizedTest
    @CsvSource({
            "/app/operator/dashboard, operator/dashboard",
            "/app/operator/receiving, operator/receiving",
            "/app/operator/placement, operator/placement",
            "/app/operator/relocation, operator/relocation",
            "/app/operator/inventory, operator/inventory"
    })
    @WithMockUser(roles = "OPERATOR")
    void rendersOperatorPages(String path, String viewName) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName));
    }

    private UsernamePasswordAuthenticationToken authentication(String role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com",
                "password",
                java.util.List.of(new SimpleGrantedAuthority(role))
        );
    }
}
