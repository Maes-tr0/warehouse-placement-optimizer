package com.nikitaopara.warehouseoptimizer.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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

    @RestController
    static class TestController {

        @GetMapping("/admin/security-test")
        String adminEndpoint() {
            return "secured";
        }
    }
}
