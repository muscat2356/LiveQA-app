package com.liveqa.dto.response;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.liveqa.domain.User;

/**
 * ログインユーザー情報GETの応答DTO
 * roleはローカルusers.role（認可用Authenticationとは別）
 * MeResponse
 * @author ichikura
 */
public record MeResponse(
    Long id,
    String displayName,
    String email,
    String role
) {

    /**
     * ローカルUserとOIDCクレームから応答DTOへ変換するファクトリ
     * displayName / email はIdToken。id / role はusers
     * @param local ローカルusers行
     * @param oidcUser OIDCクレーム
     * @return 応答DTO
     */
    public static MeResponse form(User local, OidcUser oidcUser) {
        final String displayName = oidcUser.getFullName() != null
                ? oidcUser.getFullName()
                : oidcUser.getPreferredUsername();
        return new MeResponse(
                local.getId(),
                displayName,
                oidcUser.getEmail(),
                local.getRole());
    }
}
