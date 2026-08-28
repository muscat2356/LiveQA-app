package com.liveqa.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

class EndSessionClientRegistrationRepositoryTest {

    private static final String END_SESSION =
            "http://localhost:8080/realms/live-qa/protocol/openid-connect/logout";

    @Test
    void metadataが空ならend_sessionを載せる() {
        final EndSessionClientRegistrationRepository repository = wrapping(registrationWithoutEndSession());

        final Object endpoint = repository.findByRegistrationId("keycloak")
                .getProviderDetails()
                .getConfigurationMetadata()
                .get("end_session_endpoint");

        assertThat(endpoint).isEqualTo(END_SESSION);
    }

    @Test
    void 既にあるなら上書きしない() {
        final String fromDiscovery = "http://localhost:8080/realms/live-qa/protocol/openid-connect/logout";
        final ClientRegistration discovered = ClientRegistration.withClientRegistration(registrationWithoutEndSession())
                .providerConfigurationMetadata(Map.of("end_session_endpoint", fromDiscovery))
                .build();
        final EndSessionClientRegistrationRepository repository = wrapping(discovered);

        final Object endpoint = repository.findByRegistrationId("keycloak")
                .getProviderDetails()
                .getConfigurationMetadata()
                .get("end_session_endpoint");

        assertThat(endpoint).isEqualTo(fromDiscovery);
    }

    @Test
    void HandlerはKeycloakのlogoutへ302する() throws Exception {
        final OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(wrapping(registrationWithoutEndSession()));
        handler.setPostLogoutRedirectUri("http://localhost:3000/");

        final OidcIdToken idToken = new OidcIdToken(
                "test-id-token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("sub", "host-1"));
        final DefaultOidcUser user = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_HOST")),
                idToken);
        final OAuth2AuthenticationToken authentication =
                new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak");

        final MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onLogoutSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl())
                .contains("/realms/live-qa/protocol/openid-connect/logout")
                .contains("id_token_hint=")
                .contains("post_logout_redirect_uri=");
    }

    private static EndSessionClientRegistrationRepository wrapping(ClientRegistration registration) {
        return new EndSessionClientRegistrationRepository(
                new InMemoryClientRegistrationRepository(registration),
                END_SESSION);
    }

    private static ClientRegistration registrationWithoutEndSession() {
        return ClientRegistration.withRegistrationId("keycloak")
                .clientId("live-qa-app")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("http://localhost:8080/realms/live-qa/protocol/openid-connect/auth")
                .tokenUri("http://keycloak:8080/realms/live-qa/protocol/openid-connect/token")
                .build();
    }
}
