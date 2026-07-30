package com.liveqa.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

/**
 * 匿名セッション発行（参加者用・最小）。
 * voter_key = g:{guestId}。CSRF 必須（permitAll でも POST）。
 * 出典: live-qa-session-lab GuestSession（余力）の考え方
 */
@RestController
@RequestMapping("/api/auth")
public class GuestSessionController {

    public static final String ATTR_GUEST_ID = "guestId";

    @PostMapping("/guest")
    public ResponseEntity<Map<String, String>> createGuestSession(HttpSession session) {
        Object existing = session.getAttribute(ATTR_GUEST_ID);
        String guestId = existing instanceof String s ? s : UUID.randomUUID().toString();

        session.setAttribute(ATTR_GUEST_ID, guestId);

        return ResponseEntity.ok(Map.of(
                "guestId", guestId,
                "voterKey", "g:" + guestId
        ));
    }
}
