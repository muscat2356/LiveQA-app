package com.liveqa.dto.response;

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
}
