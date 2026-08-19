package com.liveqa.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.liveqa.domain.VoterKeySource;

import jakarta.servlet.http.HttpSession;

public final class VoterKeySources {

    private VoterKeySources() {}

    public static VoterKeySource form(OidcUser oidc, HttpSession session) {

        if (oidc != null) {
            return new VoterKeySource.Host(oidc.getSubject());
        }

        return GuestSession.readGuestId(session)
                .<VoterKeySource>map(VoterKeySource.Guest::new)
                .orElseGet(VoterKeySource.Missing::new);
    }

}
