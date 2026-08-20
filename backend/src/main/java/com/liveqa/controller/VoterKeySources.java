package com.liveqa.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.liveqa.domain.VoterKeySource;

import jakarta.servlet.http.HttpSession;

/**
 * OidcUserと匿名SessionからVoterKeySourceを組み立てるstaticクラス
 * VoterKeySources
 * @author ichikura
 */
public final class VoterKeySources {

    private VoterKeySources() {}

    /**
     * 投票キーの取得元を合成するメソッド
     * HOSTがいればHost、guestIdがあればGuest、どちらも無ければMissing
     * 匿名発行（POST /api/auth/guest）には使わない
     * @param oidc SecurityContext内のOidcUser情報（未ログインならnull）
     * @param session guestIdを持つHttpSession
     * @return Host / Guest / Missing のいずれか
     */
    public static VoterKeySource form(OidcUser oidc, HttpSession session) {

        if (oidc != null) {
            return new VoterKeySource.Host(oidc.getSubject());
        }

        return fromGuestSession(session);
    }

    /**
     * 匿名SessionだけからGuest / Missingを組み立てるメソッド
     * OIDCは見ない（HOSTログイン中でもguestIdを発行できる）
     * @param session 属性を読むHttpSession（guestIdは無くてよい）
     * @return Guest または Missing
     */
    public static VoterKeySource fromGuestSession(HttpSession session) {
        return GuestSession.readGuestId(session)
                .<VoterKeySource>map(VoterKeySource.Guest::new)
                .orElseGet(VoterKeySource.Missing::new);
    }

}
