package com.nikitaopara.warehouseoptimizer.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void permitsErrorEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401));
    }

    @Test
    void stillProtectsAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/security-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permitsUiResourcesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/assets/app.css"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401));
    }

    @Test
    void redirectsProtectedUiToLogin() throws Exception {
        mockMvc.perform(get("/app/operator/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void preventsOperatorFromOpeningAdminUi() throws Exception {
        mockMvc.perform(get("/app/admin/dashboard")
                        .with(user("operator@example.com").roles("OPERATOR")))
                .andExpect(status().is3xxRedirection());
    }

    @RestController
    static class TestController {

        @GetMapping("/admin/security-test")
        String adminEndpoint() {
            return "secured";
        }
    }
}
