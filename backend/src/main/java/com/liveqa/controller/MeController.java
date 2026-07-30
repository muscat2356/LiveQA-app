package com.liveqa.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.MeResponse;

/**
 * GET /api/me … ログインユーザー情報。
 * 権限は principal（OidcUser）ではなく Authentication から読む
 * （GrantedAuthoritiesMapper の結果は Authentication に載る）。
 * 出典: live-qa-session-lab MeController（lab/ping は移植しない）
 */
@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public MeResponse me(
            @AuthenticationPrincipal OidcUser user,
            Authentication authentication
    ) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .filter(r -> r.equals("HOST"))
                .findFirst()
                .orElse("UNKNOWN");

        String displayName = user.getFullName() != null
                ? user.getFullName()
                : user.getPreferredUsername();

        return new MeResponse(
                user.getSubject(),
                displayName,
                user.getEmail(),
                role
        );
    }
}
