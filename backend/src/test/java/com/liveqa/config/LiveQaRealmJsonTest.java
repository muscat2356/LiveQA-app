package com.liveqa.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LiveQaRealmJsonTest {

    @Test
    void realmJsonにclientSecretと初期HOSTパスワードを置かない() throws IOException {
        final String json = Files.readString(realmJson(), StandardCharsets.UTF_8);

        assertThat(json).contains("\"secret\": \"__KEYCLOAK_CLIENT_SECRET__\"");
        assertThat(json).contains("\"value\": \"__KEYCLOAK_DEV_HOST_PASSWORD__\"");
        assertThat(json).doesNotContain("live-qa-dev-secret");
        assertThat(json).doesNotContain("live-qa-dev-host");
    }

    private static Path realmJson() {
        final Path fromBackend = Path.of("..", "keycloak", "live-qa-realm.json");
        if (Files.exists(fromBackend)) {
            return fromBackend;
        }
        final Path fromRoot = Path.of("keycloak", "live-qa-realm.json");
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        throw new AssertionError("keycloak/live-qa-realm.json が見つからない");
    }
}
