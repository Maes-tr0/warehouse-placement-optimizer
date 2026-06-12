package com.nikitaopara.warehouseoptimizer.auth.config;

import com.nikitaopara.warehouseoptimizer.common.error.ApiErrorResponse;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) {
        AuthenticationEntryPoint apiAuthenticationEntryPoint = (request, response, exception) -> {
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
        };

        AccessDeniedHandler apiAccessDeniedHandler = (request, response, exception) -> {
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
        };

        RequestMatcher apiRequestMatcher = request -> {
            String path = request.getRequestURI();
            return path.startsWith("/admin/")
                    || path.startsWith("/operator/")
                    || path.startsWith("/actuator/");
        };

        AuthenticationEntryPoint loginAuthenticationEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/login");

        AuthenticationEntryPoint authenticationEntryPoint = (request, response, exception) -> {
            if (apiRequestMatcher.matches(request)) {
                apiAuthenticationEntryPoint.commence(request, response, exception);
                return;
            }
            loginAuthenticationEntryPoint.commence(request, response, exception);
        };

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers("/error")
                        .permitAll()

                        .requestMatchers(
                                "/login",
                                "/assets/**",
                                "/favicon.svg",

                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        )
                        .permitAll()

                        .requestMatchers("/actuator/health", "/actuator/info")
                        .permitAll()

                        .requestMatchers("/actuator/**")
                        .hasAnyRole("ROOT_ADMIN", "ADMIN")

                        .requestMatchers("/admin/**")
                        .hasAnyRole("ROOT_ADMIN", "ADMIN")

                        .requestMatchers("/operator/**")
                        .hasAnyRole("ROOT_ADMIN", "ADMIN", "OPERATOR")

                        .requestMatchers("/app/admin/**")
                        .hasAnyRole("ROOT_ADMIN", "ADMIN")

                        .requestMatchers("/app/operator/**")
                        .hasAnyRole("ROOT_ADMIN", "ADMIN", "OPERATOR")

                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler((request, response, exception) -> {
                            if (apiRequestMatcher.matches(request)) {
                                apiAccessDeniedHandler.handle(request, response, exception);
                                return;
                            }
                            response.sendRedirect("/access-denied");
                        })
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(roleAwareSuccessHandler())
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .build();
    }

    @Bean
    public AuthenticationSuccessHandler roleAwareSuccessHandler() {
        return (request, response, authentication) ->
                response.sendRedirect(defaultPage(authentication));
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    private String defaultPage(Authentication authentication) {
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ROOT_ADMIN")
                        || authority.getAuthority().equals("ROLE_ADMIN"));

        return administrator ? "/app/admin/dashboard" : "/app/operator/dashboard";
    }
}