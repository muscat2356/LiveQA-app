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

/**
 * GET /api/me … ログインユーザー情報。
 * 権限は principal（OidcUser）ではなく Authentication から読む
 * （GrantedAuthoritiesMapper の結果は Authentication に載る）。
 * 出典: live-qa-session-lab MeController（lab/ping は移植しない）
 * @author ichikura
 */
@RestController
@RequestMapping("/api")
public class MeController {

private final UserService userService;
    
public MeController(UserService userService) {
        this.userService = userService;
}

/**
* セッション情報を返すGETメソッド
* @param oidcUser Authenticationの中のOidcUserprincipalで直接受け取る
* @return 200 ユーザー情報のResponse
*/
@GetMapping("/me")
@PreAuthorize("hasRole('HOST')")
public ResponseEntity<MeResponse> me(
    @AuthenticationPrincipal OidcUser oidcUser
) {
        return ResponseEntity.ok(userService.currentHost(oidcUser));
}
}
