package com.liveqa.client;

/**
 * Keycloak Admin API の契約。ローカル users は触らない
 * KeycloakAdminClient
 * @author ichikura
 */
public interface KeycloakAdminClient {

    String createHostUser(String email);

    void assignHostRole(String userId);

    void sendUpdatePasswordEmail(String userId);
}
