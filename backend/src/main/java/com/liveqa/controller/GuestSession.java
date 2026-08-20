package com.liveqa.controller;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;

/**
 * Session（Redis）からguestIdの抽出、書き込みメソッドを持ったstaticクラス
 * GuestSession
 * @author ichikura
 */
public final class GuestSession {

    public static final String ATTR_GUEST_ID = "guestId";

    private GuestSession() {}

    /**
     * guestIdの抽出を行うメソッド
     * 型安全の為Optionalでラップして返却
     * @param session 属性を読むHttpSession（guestIdは無くてよい）
     * @return あればguestId、無ければempty（このメソッド自体はnullを返さない）
     */
    public static Optional<String> readGuestId(HttpSession session) {
        final Object existing = session.getAttribute(ATTR_GUEST_ID);
        if (existing instanceof String guestId && !guestId.isBlank()) {
            return Optional.of(guestId);
        }
        return Optional.empty();
    }

    /**
     * guestIdの書き込みを行うメソッド
     * @param session 書き込み対象
     * @param guestId 書き込むguestId
     */
    public static void writeGuestId(HttpSession session, String guestId) {
        session.setAttribute(ATTR_GUEST_ID, guestId);
    }
}
