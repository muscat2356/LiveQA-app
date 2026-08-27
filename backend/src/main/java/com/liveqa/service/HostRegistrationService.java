package com.liveqa.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.liveqa.client.KeycloakAdminClient;

/**
 * HOST メール発行のユースケース。ローカル users は作らない
 * HostRegistrationService
 * @author ichikura
 */
@Service
public class HostRegistrationService {

    private final KeycloakAdminClient keycloakAdminClient;

    public HostRegistrationService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    /**
     * Keycloak に HOST を格納し、パスワード設定メールを発行するメソッド
     * @param email 入力メール
     */
    public void register(String email) {
        final String normalized = email.trim().toLowerCase(Locale.ROOT);
        final String userId = keycloakAdminClient.createHostUser(normalized);
        keycloakAdminClient.assignHostRole(userId);
        keycloakAdminClient.sendUpdatePasswordEmail(userId);
    }
}
