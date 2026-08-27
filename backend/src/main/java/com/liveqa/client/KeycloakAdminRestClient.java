package com.liveqa.client;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.liveqa.exception.BusinessException;

/**
 * Keycloak Admin REST の実装。master の管理者でトークンを交換し、live-qa に HOST を発行する。
 * ローカル users は触らない。パスワードは受け取らない。
 * KeycloakAdminRestClient
 * @author ichikura
 */
@Component
public class KeycloakAdminRestClient implements KeycloakAdminClient {

    private static final String FAIL_MESSAGE = "ユーザー発行に失敗しました";

    private final RestClient restClient;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;
    private final String actionsClientId;
    private final String actionsRedirectUri;

    public KeycloakAdminRestClient(
            @Value("${app.keycloak.admin.base-url}") String baseUrl,
            @Value("${app.keycloak.admin.realm}") String realm,
            @Value("${app.keycloak.admin.username}") String adminUsername,
            @Value("${app.keycloak.admin.password}") String adminPassword,
            @Value("${app.keycloak.admin.client-id}") String actionsClientId,
            @Value("${app.keycloak.admin.redirect-uri}") String actionsRedirectUri) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.actionsClientId = actionsClientId;
        this.actionsRedirectUri = actionsRedirectUri;
    }

    /**
     * Keycloak に HOST 用ユーザーを格納するメソッド
     * 同じメールなら409。Location から user id を復元する
     * @param email 正規化済みメール（username と email に同じ値を載せる）
     * @return Keycloak の user id
     */
    @Override
    public String createHostUser(String email) {
        try {
            final ResponseEntity<Void> created = restClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "username", email,
                    "email", email,
                    "enabled", true,
                    "emailVerified", true
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        throw new BusinessException(
                            HttpStatus.CONFLICT,
                            "Conflict",
                            "このメールアドレスは既に登録されています。"
                        );
                    }
                    throw badGateway();
                })
                .toBodilessEntity();
            return userIdFromLocation(created.getHeaders().getLocation());
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw badGateway();
        }
    }

    /**
     * 指定ユーザーに realm role HOST を付けるメソッド
     * 先に HOST ロール定義を復元し、role-mappings へ格納する
     * @param userId Keycloak の user id
     */
    @Override
    public void assignHostRole(String userId) {
        try {
            final RealmRole host = restClient.get()
                    .uri("/admin/realms/{realm}/roles/HOST", realm)
                    .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw badGateway();
                    })
                    .body(RealmRole.class);
            if (host == null || host.id() == null) {
                throw badGateway();
            }
            restClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                    .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(host))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw badGateway();
                    })
                    .toBodilessEntity();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw badGateway();
        }
    }

    /**
     * パスワード設定リンクメールを発行するメソッド
     * 本文は Keycloak が SMTP で送る。一次パスワードは載せない
     * @param userId Keycloak の user id
     */
    @Override
    public void sendUpdatePasswordEmail(String userId) {
        try {
            restClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                            .queryParam("client_id", actionsClientId)
                            .queryParam("redirect_uri", actionsRedirectUri)
                            .build(realm, userId))
                    .headers(headers -> headers.setBearerAuth(adminAccessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of("UPDATE_PASSWORD"))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw badGateway();
                    })
                    .toBodilessEntity();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw badGateway();
        }
    }

    /**
     * master の admin-cli で管理者トークンを交換するメソッド
     * 主催者ログイン（live-qa-app）とは別の鍵
     * @return Bearer に載せる access_token
     */
    private String adminAccessToken() {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);
        final TokenResponse token = restClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw badGateway();
                })
                .body(TokenResponse.class);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw badGateway();
        }
        return token.accessToken();
    }

    /**
     * 201 の Location ヘッダから user id を復元するメソッド
     * @param location Keycloak が返す Location
     * @return パス末尾の id
     */
    private String userIdFromLocation(URI location) {
        if (location == null || location.getPath() == null) {
            throw badGateway();
        }
        final String path = location.getPath();
        final int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            throw badGateway();
        }
        return path.substring(lastSlash + 1);
    }

    /**
     * Admin API / SMTP 失敗を 502 にするメソッド
     * Keycloak の生エラーは返さない
     * @return Bad Gateway の業務例外
     */
    private static BusinessException badGateway() {
        return new BusinessException(
                HttpStatus.BAD_GATEWAY,
                "Bad Gateway",
                FAIL_MESSAGE);
    }

    /**
     * トークン応答の必要な項目だけ。他の JSON は無視する
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    /**
     * realm role の id と name。role-mappings の本文に載せる
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RealmRole(String id, String name) {
    }

}
