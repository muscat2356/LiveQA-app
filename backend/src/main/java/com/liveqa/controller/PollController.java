package com.liveqa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.request.PollCreateRequest;
import com.liveqa.dto.request.PollRespondRequest;
import com.liveqa.dto.request.PollStatusRequest;
import com.liveqa.dto.response.PollResponse;
import com.liveqa.service.PollService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * リソースpollsに対するAPIコントローラー
 * 作成・一覧・開閉・回答・結果
 * PollController
 * @author ichikura
 */
@RestController
@RequestMapping("/api")
@Tag(name = "ポール", description = "作成・一覧・開閉。回答・結果は F-11")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    /**
     * ポール作成を行うPOSTメソッド
     * 所有者のみ。新規はDRAFT固定
     * @param id イベントid
     * @param oidc SecurityContext内のOidcUser情報
     * @param request 設問文・種別・選択肢
     * @return 作成したポール
     */
    @PostMapping("/events/{id}/polls")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PollResponse> create(
        @PathVariable Long id,
        @AuthenticationPrincipal OidcUser oidc,
        @Valid @RequestBody PollCreateRequest request
    
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(pollService.create(id, request, oidc));
    }

    /**
     * イベント配下のポール一覧を返すGETメソッド
     * 所有者は全件、それ以外は OPEN。未ログインでも 200
     * @param id イベントid
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     * @return ポール一覧
     */
    @GetMapping("/events/{id}/polls")
    public ResponseEntity<List<PollResponse>> findByEventId(
            @PathVariable Long id,
            @AuthenticationPrincipal OidcUser oidc
    ) {
        return ResponseEntity.ok(pollService.findByEventId(id, oidc));
    }

    /**
     * ポールステータスを遷移するPATCHメソッド
     * 受け付ける値と遷移可否の検証はService
     * @param oidc SecurityContext内のOidcUser情報
     * @param pollId ポールid
     * @param request OPEN または CLOSED
     * @return 更新したポール
     */
    @PatchMapping("/polls/{pollId}/status")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PollResponse> updateStatus(
            @AuthenticationPrincipal OidcUser oidc,
            @PathVariable Long pollId,
            @Valid @RequestBody PollStatusRequest request
    ) {
        return ResponseEntity.ok(
                pollService.updateStatus(pollId, request.status(), oidc));
    }

    /**
     * ポールへ回答するPOSTメソッド
     * ゲスト/HOST。voter_keyはSession。CSRF必須
     * @param pollId ポールid
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     * @param session guestIdを持つHttpSession
     * @param request 選んだ選択肢id
     * @return 最新集計のポール
     */
    @PostMapping("/polls/{pollId}/responses")
    public ResponseEntity<PollResponse> respond(
            @PathVariable Long pollId,
            @AuthenticationPrincipal OidcUser oidc,
            HttpSession session,
            @Valid @RequestBody PollRespondRequest request
    ) {
        return ResponseEntity.ok(
                pollService.respond(
                        pollId,
                        request,
                        VoterKeySources.form(oidc, session)));
    }

    /**
     * ポールの得票を返すGETメソッド
     * 公開。HOST以外のDRAFTは404。CSRF不要
     * @param pollId ポールid
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     * @param session guestIdを持つHttpSession
     * @return 最新集計のポール
     */
    @GetMapping("/polls/{pollId}/results")
    public ResponseEntity<PollResponse> findResults(
            @PathVariable Long pollId,
            @AuthenticationPrincipal OidcUser oidc,
            HttpSession session
    ) {
        return ResponseEntity.ok(
                pollService.findResults(
                        pollId,
                        oidc,
                        VoterKeySources.form(oidc, session)));
    }

}
