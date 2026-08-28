package com.liveqa.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ApplicationDockerYmlTest {

    @Test
    void authorizationUriはenvでtokenUriはコンテナ名のまま() throws IOException {
        final String yaml = new String(
                new ClassPathResource("application-docker.yml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(yaml).contains(
                "authorization-uri: ${KEYCLOAK_AUTHORIZATION_URI:http://localhost:8080/realms/live-qa/protocol/openid-connect/auth}");
        assertThat(yaml).contains(
                "token-uri: http://keycloak:8080/realms/live-qa/protocol/openid-connect/token");
    }
}
