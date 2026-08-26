package com.liveqa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.response.MeResponse;
import com.liveqa.service.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * リソースmeに対するAPIコントローラー（ログインユーザー情報）
 * 権限はprincipal（OidcUser）ではなくAuthenticationから読む
 * MeController
 * @author ichikura
 */
@RestController
@RequestMapping("/api")
@Tag(name = "認証", description = "GET /api/me。logout は Security の /api/auth/logout（RestController ではない）")
@SecurityRequirement(name = "SESSION")
public class MeController {

private final UserService userService;
    
public MeController(UserService userService) {
        this.userService = userService;
}

/**
 * ログインユーザー情報を返すGETメソッド
 * @param oidcUser Authenticationの中のOidcUser。principalで直接受け取る
 * @return ユーザー情報
 */
@GetMapping("/me")
@PreAuthorize("hasRole('HOST')")
public ResponseEntity<MeResponse> me(
    @AuthenticationPrincipal OidcUser oidcUser
) {
        return ResponseEntity.ok(userService.currentHost(oidcUser));
}
}
