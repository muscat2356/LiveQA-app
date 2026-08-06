package com.liveqa.dto.response;

/**
 * GET /api/me 応答。openapi-notes の認証節に合わせる。
 */
public record MeResponse(
    Long id,
    String displayName,
    String email,
    String role
) {
}
