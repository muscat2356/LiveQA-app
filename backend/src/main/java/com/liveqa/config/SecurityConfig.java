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
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveqa.service.UserService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * F-01: oauth2Login（Keycloak）→ セッション / CSRF / RBAC。
 * 出典: live-qa-session-lab SecurityConfig（lab/ping は移植しない）
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ObjectMapper objectMapper,
                        UserService userService,
                        ClientRegistrationRepository clientRegistrationRepository
    ) {
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.clientRegistrationRepository = clientRegistrationRepository;
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
                .requestMatchers("/api/me","/api/auth/logout").authenticated()
                .anyRequest().permitAll()
            )
            // Bean だけでは呼ばれない → userAuthoritiesMapper で差し込む
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
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
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
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
            -> writeJson(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "ログインが必要です");
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException)
            -> writeJson(response, HttpStatus.FORBIDDEN, "Forbidden", "権限がありません");
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

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        final OidcClientInitiatedLogoutSuccessHandler handler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);

            handler.setPostLogoutRedirectUri("{baseUrl}/");
            return handler;
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            userService.provisionHost(oidcUser.getSubject());
            return oidcUser;
        };
    }
}
