package com.liveqa.controller;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;

public final class GuestSession {

    public static final String ATTR_GUEST_ID = "guestId";

    private GuestSession() {}

    public static Optional<String> readGuestId(HttpSession session) {
        final Object existing = session.getAttribute(ATTR_GUEST_ID);
        if (existing instanceof String guestId && !guestId.isBlank()) {
            return Optional.of(guestId);
        }
        return Optional.empty();
    }

    public static void writeGuestId(HttpSession session, String guestId) {
        session.setAttribute(ATTR_GUEST_ID, guestId);
    }
}
