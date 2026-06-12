package com.nikitaopara.warehouseoptimizer.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI warehouseOptimizerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Warehouse Optimizer API")
                        .version("1.0.0")
                        .description("""
                                REST API for intelligent warehouse placement and relocation optimization.

                                The system supports warehouse generation, article and pallet management,
                                demand history import, AI-based demand forecasting, placement recommendations,
                                optimization assessment, relocation planning and operator execution flow.
                                """)
                        .license(new License()
                                .name("Academic / Diploma Project")))
                .tags(List.of(
                        new Tag().name("Accounts").description("Admin and operator account management"),
                        new Tag().name("Warehouses").description("Warehouse creation, layout generation and storage places"),
                        new Tag().name("Articles").description("Article master data management"),
                        new Tag().name("Containers").description("Receiving, placing, merging and removing pallets/containers"),
                        new Tag().name("Placement").description("Placement recommendation workflow"),
                        new Tag().name("Demand History").description("Historical order demand import"),
                        new Tag().name("Demand Analytics").description("Article demand analytics and popularity"),
                        new Tag().name("Demand Forecast Models").description("AI/ML demand model training and history"),
                        new Tag().name("Optimization Assessments").description("Warehouse optimization score calculation"),
                        new Tag().name("Optimization Plans").description("Relocation plan creation and management"),
                        new Tag().name("Relocation Execution").description("Operator relocation step execution"),
                        new Tag().name("Movement History").description("Container movement history"),
                        new Tag().name("Audit Events").description("Searchable warehouse audit events")
                ))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Use application email and password. Admin endpoints require ROOT_ADMIN or ADMIN. Operator endpoints require OPERATOR.")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}