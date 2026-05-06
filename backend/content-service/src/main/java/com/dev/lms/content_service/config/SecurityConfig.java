package com.dev.lms.content_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .addFilterAt(gatewayHeadersFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/api/content/files/**").permitAll()
                        .pathMatchers("/api/content/upload/**").hasRole("INSTRUCTOR")
                        .anyExchange().authenticated()
                )
                .build();
    }

    private AuthenticationWebFilter gatewayHeadersFilter() {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(
                (ReactiveAuthenticationManager) Mono::just
        );
        filter.setServerAuthenticationConverter(this::extractAuthentication);
        return filter;
    }

    private Mono<Authentication> extractAuthentication(ServerWebExchange exchange) {
        String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

        if (role == null || role.isBlank() || userId == null || userId.isBlank()) {
            return Mono.empty();
        }

        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) normalized = normalized.substring(5);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + normalized));
        return Mono.just(new UsernamePasswordAuthenticationToken(userId, null, authorities));
    }
}
