package com.nikitaopara.warehouseoptimizer.ui.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
            "/app/admin/accounts, admin/accounts",
            "/app/admin/movements, admin/movements",
            "/app/admin/audit, admin/audit"
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

    @Test
    @WithMockUser(roles = "ROOT_ADMIN")
    void rendersDynamicWarehouseLevelControlsForRootAdministrator() throws Exception {
        mockMvc.perform(get("/app/admin/warehouse"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"rackLevelCount\"")))
                .andExpect(content().string(containsString("id=\"levelProfiles\"")))
                .andExpect(content().string(containsString("max=\"4\"")))
                .andExpect(content().string(containsString("+ Create warehouse")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void hidesActionsThatAdministratorCannotPerform() throws Exception {
        mockMvc.perform(get("/app/admin/warehouse"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("+ Create warehouse"))));

        mockMvc.perform(get("/app/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("value=\"ADMIN\""))));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void hidesInventoryManagementControlsFromOperator() throws Exception {
        mockMvc.perform(get("/app/operator/inventory"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("containerManagementPermission"))))
                .andExpect(content().string(not(containsString("id=\"containerDialog\""))))
                .andExpect(content().string(not(containsString(">Actions</th>"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void showsInventoryManagementControlsInOperatorModeForAdministrator() throws Exception {
        mockMvc.perform(get("/app/operator/inventory"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("containerManagementPermission")))
                .andExpect(content().string(containsString("id=\"containerDialog\"")))
                .andExpect(content().string(containsString(">Actions</th>")));
    }

    private UsernamePasswordAuthenticationToken authentication(String role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com",
                "password",
                java.util.List.of(new SimpleGrantedAuthority(role))
        );
    }
}
