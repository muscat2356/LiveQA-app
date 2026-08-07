package com.liveqa.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
    private final String loginSuccessRedirectUri;
    private final String postLogoutRedirectUri;
    private final List<String> corsAllowedOrigins;

    public SecurityConfig(ObjectMapper objectMapper,
                        UserService userService,
                        ClientRegistrationRepository clientRegistrationRepository,
                        @Value("${app.login.success-redirect-uri}") String loginSuccessRedirectUri,
                        @Value("${app.logout.post-logout-redirect-uri}") String postLogoutRedirectUri,
                        @Value("${app.cors.allowed-origins}") List<String> corsAllowedOrigins
    ) {
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.loginSuccessRedirectUri = loginSuccessRedirectUri;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/auth/guest").permitAll()
                .requestMatchers("/api/me", "/api/auth/logout", "/api/events", "/api/events/**").authenticated()
                .anyRequest().permitAll()
            )
            // Bean だけでは呼ばれない → userAuthoritiesMapper で差し込む
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
                    .userAuthoritiesMapper(grantedAuthoritiesMapper())
                )
                .successHandler(loginSuccessHandler())
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> {
            final Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            for (GrantedAuthority authority : authorities) {
                if (authority instanceof OidcUserAuthority oidc) {
                    // Keycloak の realm role は IdToken の realm_access に載せる前提で一本化する
                    addRolesFromRealmAccess(oidc.getIdToken().getClaimAsMap("realm_access"), mapped);
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
        return (request, response, accessDeniedException) -> {
            // CSRF 失敗も AccessDeniedException 配下のため、欠落／不一致を分けて返す
            if (accessDeniedException instanceof MissingCsrfTokenException) {
                writeJson(response, HttpStatus.FORBIDDEN, "Forbidden", "CSRFトークンがありません");
                return;
            }
            if (accessDeniedException instanceof InvalidCsrfTokenException) {
                writeJson(response, HttpStatus.FORBIDDEN, "Forbidden", "CSRFトークンが一致しません");
                return;
            }
            writeJson(response, HttpStatus.FORBIDDEN, "Forbidden", "権限がありません");
        };
    }

    private void writeJson(
        HttpServletResponse response,
        HttpStatus status,
        String error,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        // getWriter() 前に必須。未設定だと Servlet 既定（ISO-8859-1）になり日本語が化ける
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "status", status.value(),
            "error", error,
            "message", message
        ));
    }

    private AuthenticationSuccessHandler loginSuccessHandler() {
        final SimpleUrlAuthenticationSuccessHandler handler =
            new SimpleUrlAuthenticationSuccessHandler(loginSuccessRedirectUri);
        handler.setDefaultTargetUrl(loginSuccessRedirectUri);
        handler.setAlwaysUseDefaultTargetUrl(true);
        return handler;
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        final OidcClientInitiatedLogoutSuccessHandler handler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);

            handler.setPostLogoutRedirectUri(postLogoutRedirectUri);
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
