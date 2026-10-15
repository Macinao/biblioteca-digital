package com.digital.smartteca.storage.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeaderAuthWebFilter gatewayHeaderAuthWebFilter;
    private final CorrelationIdWebFilter correlationIdWebFilter;

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .pathMatchers("/api/v1/**").authenticated()
                        .anyExchange().denyAll()
                )
                .addFilterAt(gatewayHeaderAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAt(correlationIdWebFilter, SecurityWebFiltersOrder.FIRST)
                .build();

    }
}
