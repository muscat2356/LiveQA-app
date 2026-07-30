package com.liveqa.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

/**
 * F-01: oauth2Login（Keycloak）→ セッション / CSRF / RBAC。
 * 出典: live-qa-session-lab SecurityConfig（lab/ping は移植しない）
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/guest").permitAll()
                .requestMatchers("/api/me").authenticated()
                .anyRequest().permitAll()
            )
            // Bean だけでは呼ばれない → userAuthoritiesMapper で差し込む
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userAuthoritiesMapper(grantedAuthoritiesMapper())
                )
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepo)
                .csrfTokenRequestHandler(csrfHandler)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }

    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            for (GrantedAuthority authority : authorities) {
                if (authority instanceof OidcUserAuthority oidc) {
                    addRolesFromRealmAccess(oidc.getIdToken().getClaimAsMap("realm_access"), mapped);
                    if (oidc.getUserInfo() != null) {
                        addRolesFromRealmAccess(oidc.getUserInfo().getClaimAsMap("realm_access"), mapped);
                    }
                    Object attr = oidc.getAttributes().get("realm_access");
                    if (attr instanceof Map<?, ?> raw) {
                        Map<String, Object> realmAccess = new HashMap<>();
                        raw.forEach((k, v) -> {
                            if (k instanceof String key) {
                                realmAccess.put(key, v);
                            }
                        });
                        addRolesFromRealmAccess(realmAccess, mapped);
                    }
                }
            }
            return mapped;
        };
    }

    private void addRolesFromRealmAccess(Map<String, Object> realmAccess, Set<GrantedAuthority> dest) {
        if (realmAccess == null) {
            return;
        }
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> roles)) {
            return;
        }
        for (Object role : roles) {
            if (role instanceof String name) {
                dest.add(new SimpleGrantedAuthority("ROLE_" + name));
            }
        }
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException)
            -> writeJson(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "ログインが必須です");
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException)
            -> writeJson(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "権限がありません");
    }

    private void writeJson(
        HttpServletResponse response,
        HttpStatus status,
        String error,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "status", status.value(),
            "error", error,
            "message", message
        ));
    }
}
