package com.nikitaopara.warehouseoptimizer.auth.config;

import com.nikitaopara.warehouseoptimizer.common.error.ApiErrorResponse;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers("/error")
                        .permitAll()

                        .requestMatchers("/admin/**")
                        .hasAnyRole("ROOT_ADMIN", "ADMIN")

                        .requestMatchers("/operator/**")
                        .hasAnyRole("ROOT_ADMIN", "ADMIN", "OPERATOR")

                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    ApiErrorResponse.of(
                                            HttpStatus.UNAUTHORIZED.value(),
                                            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                            "AUTHENTICATION_REQUIRED",
                                            "Valid authentication credentials are required",
                                            request.getRequestURI()
                                    )
                            );
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    ApiErrorResponse.of(
                                            HttpStatus.FORBIDDEN.value(),
                                            HttpStatus.FORBIDDEN.getReasonPhrase(),
                                            "ACCESS_DENIED",
                                            "You are not allowed to perform this action",
                                            request.getRequestURI()
                                    )
                            );
                        })
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
