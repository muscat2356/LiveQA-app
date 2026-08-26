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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.request.QuestionCreateRequest;
import com.liveqa.dto.request.QuestionStatusRequest;
import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.service.QuestionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * リソースquestionsに対するAPIコントローラー
 * QuestionController
 * @author ichikura
 */
@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 質問投稿を行うPOSTメソッド
     * @param id イベントid
     * @param request 質問本文と任意ニックネーム
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     * @param session 匿名guestIdを持つHttpSession
     * @return 作成した質問情報
     */
    @PostMapping("/events/{id}/questions")
    public ResponseEntity<QuestionResponse> create(
            @PathVariable Long id,
            @Valid @RequestBody QuestionCreateRequest request,
            @AuthenticationPrincipal OidcUser oidc,
            HttpSession session
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                questionService
                    .create(
                        id,
                        request,
                        VoterKeySources.form(oidc, session)
                    )
                );
        }
    
    /**
     * イベント配下の質問一覧を返すGETメソッド
     * votedByMeは今のセッション視点。Sessionが無くても200
     * sort / status の検証はService（未指定のstatusはARCHIVED以外）
     * @param id イベントid
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     * @param sort 並び（popular / newest）。未指定可
     * @param status フィルタ（OPEN / ANSWERED / ARCHIVED）。未指定可
     * @param session 匿名guestIdを持つHttpSession
     * @return 質問一覧（List）
     */
    @GetMapping("/events/{id}/questions")
    public ResponseEntity<List<QuestionResponse>> findByEventId(
        @PathVariable Long id,
        @AuthenticationPrincipal OidcUser oidc,
        @RequestParam(name = "sort", required = false) String sort,
        @RequestParam(name = "status", required = false) String status,
        HttpSession session
    ) {
        return ResponseEntity.ok(
            questionService.findByEventId(
                    id,
                    sort,
                    status,
                    VoterKeySources.form(oidc, session))
        );
    }
    
    /**
     * 質問ステータスを遷移するPATCHメソッド
     * ステータスの検証はservice
     */
    @PatchMapping("/questions/{qid}/status")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<QuestionResponse> updateStatus(
            @AuthenticationPrincipal OidcUser oidc,
            @PathVariable Long qid,
            @Valid @RequestBody QuestionStatusRequest request,
            HttpSession session
    ) {
        return ResponseEntity.ok(
                questionService.updateStatus(
                        qid,
                        request.status(),
                        oidc,
                        VoterKeySources.form(oidc, session)));
    }

}
