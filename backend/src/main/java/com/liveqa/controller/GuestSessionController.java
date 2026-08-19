package com.liveqa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liveqa.dto.response.GuestResponse;
import com.liveqa.service.GuestSessionService;

import jakarta.servlet.http.HttpSession;

/**
 * 匿名セッション発行（参加者用・最小）。
 * voter_key = g:{guestId}。CSRF 必須（permitAll でも POST）。
 * 出典: live-qa-session-lab GuestSession（余力）の考え方
 */
@RestController
@RequestMapping("/api/auth")
public class GuestSessionController {

    private final GuestSessionService guestSessionService;

    public GuestSessionController(GuestSessionService guestSessionService) {
        this.guestSessionService = guestSessionService;
    }

    @PostMapping("/guest")
    public ResponseEntity<GuestResponse> createGuestSession(HttpSession session) {
        final GuestResponse response =
         guestSessionService.issueOrReuse(GuestSession.readGuestId(session));
         GuestSession.writeGuestId(session, response.guestId());
        return ResponseEntity.ok(response);
    }
}
