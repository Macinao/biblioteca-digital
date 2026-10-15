package com.digital.smartteca.search.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/**
 * Sem rotas publicas de auth aqui (quem faz login e o core-service) e
 * sem CORS (browser nunca fala com o search-service direto, so via
 * gateway) — por isso essa config e bem mais enxuta que a do core-service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;
    private final CorrelationIdFilter correlationIdFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(correlationIdFilter, DisableEncodeUrlFilter.class)
                .build();

    }
}
