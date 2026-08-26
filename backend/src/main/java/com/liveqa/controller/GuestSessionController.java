package com.liveqa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.response.GuestResponse;
import com.liveqa.service.GuestSessionService;

import jakarta.servlet.http.HttpSession;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * リソースguestに対するAPIコントローラー（匿名セッション発行）
 * CSRF検証はしない（発行専用。応答で XSRF-TOKEN が載る）
 * GuestSessionController
 * @author ichikura
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "認証", description = "匿名セッション POST /api/auth/guest")
public class GuestSessionController {

    private final GuestSessionService guestSessionService;

    public GuestSessionController(GuestSessionService guestSessionService) {
        this.guestSessionService = guestSessionService;
    }

    /**
     * 匿名セッションの発行または再利用を行うPOSTメソッド
     * 組み立てはfromGuestSession（formは使わない）
     * @param session guestIdを読み書きするHttpSession
     * @return guestIdとvoterKey
     */
    @PostMapping("/guest")
    public ResponseEntity<GuestResponse> createGuestSession(HttpSession session) {
        final GuestResponse response =
         guestSessionService.issueOrReuse(VoterKeySources.fromGuestSession(session));
         GuestSession.writeGuestId(session, response.guestId());
        return ResponseEntity.ok(response);
    }
}
