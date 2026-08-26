package com.liveqa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.response.VoteResponse;
import com.liveqa.service.VoteService;

import jakarta.servlet.http.HttpSession;
import io.swagger.v3.oas.annotations.tags.Tag;
/**
 * リソースvoteに対するAPIコントローラー
 * VoteController
 * @author ichikura
 */
@RestController
@RequestMapping("/api")
@Tag(name = "いいね", description = "追加・取消")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    /**
     * いいねを追加するPOSTメソッド
     * CSRF必須
     * @param qid 対象の質問id
     * @param oidc SecurityContext内のOidcUser情報
     * @param session guestIdを読み書きするHttpSession
     * @return いいねされた情報を含むResponseクラス
     */
    @PostMapping("/questions/{qid}/vote")
    public ResponseEntity<VoteResponse> add(
        @PathVariable Long qid,
        @AuthenticationPrincipal OidcUser oidc,
        HttpSession session
    ) {
        return ResponseEntity
            .ok(voteService.add(
                qid,
                VoterKeySources.form(oidc, session)
            ));
    }

    /**
     * いいねを取り消すDELETEメソッド
     * CSRF必須
     * @param qid 対象の質問id
     * @param oidc SecurityContext内のOidcUser情報
     * @param session guestIdを読み書きするHttpSession
     * @return いいねが削除された情報を含むResponseクラス
     */
    @DeleteMapping("/questions/{qid}/vote")
    public ResponseEntity<VoteResponse> remove(
        @PathVariable Long qid,
        @AuthenticationPrincipal OidcUser oidc,
        HttpSession session
    ) {
        return ResponseEntity
            .ok(voteService.remove(
                qid,
                VoterKeySources.form(oidc, session)
            ));
    }

}
