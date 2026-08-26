package com.liveqa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.request.EventCreateRequest;
import com.liveqa.dto.request.EventStatusRequest;
import com.liveqa.dto.request.EventTitleRequest;
import com.liveqa.dto.response.EventCreateResponse;
import com.liveqa.dto.response.EventDetailResponse;
import com.liveqa.dto.response.EventStatusResponse;
import com.liveqa.dto.response.EventSummaryResponse;
import com.liveqa.service.EventService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * リソースeventsに対するAPIコントローラー
 * EventController
 * @author ichikura
 */
@RestController
@RequestMapping("/api")
@Tag(name = "イベント", description = "作成・一覧・詳細・名称・削除・開閉")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 自分のイベント一覧を返すGETメソッド
     * @param oidc SecurityContext内のOidcUser情報
     * @return イベント一覧情報（List）
     */
    @GetMapping("/events")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<List<EventSummaryResponse>> findAll(@AuthenticationPrincipal OidcUser oidc) {
        return ResponseEntity.ok(eventService.findAll(oidc));
    }

    /**
     * イベント作成を行うPOSTメソッド
     * @param oidc SecurityContext内のOidcUser情報
     * @param request イベント作成時の入力内容
     * @return 作成したイベント情報
     */
    @PostMapping("/events")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<EventCreateResponse> create(
            @AuthenticationPrincipal OidcUser oidc,
            @Valid @RequestBody EventCreateRequest request
        ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.save(oidc, request.title()));
    }

    /**
     * id指定でイベント詳細を返すGETメソッド
     * @param oidc SecurityContext内のOidcUser情報
     * @param id イベントid
     * @return 抽出したイベント詳細情報
     */
    @GetMapping("/events/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<EventDetailResponse> findById(
            @AuthenticationPrincipal OidcUser oidc,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventService.findById(id, oidc));
    }

    /**
     * ルームステータスを遷移するPATCHメソッド
     * ステータスの検証はservice
     * @param oidc SecurityContext内のOidcUser情報
     * @param id イベントid
     * @param request 遷移したいステータス
     * @return 遷移後のステータス情報
     */
    @PatchMapping("/events/{id}/status")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<EventStatusResponse> updateStatus(
        @AuthenticationPrincipal OidcUser oidc,
        @PathVariable Long id,
        @Valid @RequestBody EventStatusRequest request
    ) {
        return ResponseEntity.ok(eventService.updateStatus(id, request.status(), oidc));
    }

    /**
     * イベント名を更新するPATCHメソッド
     * 桁数の検証は Request。業務判定は service
     * @param oidc SecurityContext内のOidcUser情報
     * @param id イベントid
     * @param request 新しい名称
     * @return 更新後のイベント詳細
     */
    @PatchMapping("/events/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<EventDetailResponse> updateTitle(
        @AuthenticationPrincipal OidcUser oidc,
        @PathVariable Long id,
        @Valid @RequestBody EventTitleRequest request
    ) {
        return ResponseEntity.ok(eventService.updateTitle(id, request.title(), oidc));
    }

    /**
     * イベントを物理削除するDELETEメソッド
     * 成功は204。業務判定はservice
     */
    @DeleteMapping("/events/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal OidcUser oidc,
            @PathVariable Long id
    ) {
        eventService.delete(id, oidc);
        return ResponseEntity.noContent().build();
    }

}
