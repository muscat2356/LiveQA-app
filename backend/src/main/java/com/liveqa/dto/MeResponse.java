package com.liveqa.dto;

/**
 * GET /api/me 応答。openapi-notes の認証節に合わせる。
 */
public record MeResponse(
    String id,
    String displayName,
    String email,
    String role
) {
}
