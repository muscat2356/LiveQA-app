package com.liveqa.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.liveqa.domain.VoterKeySource;

class VoterKeySourcesTest {

    @Test
    void form_oidcありならHost() {
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        final VoterKeySource source = VoterKeySources.form(oidc, session);

        assertThat(source).isEqualTo(new VoterKeySource.Host("sub-1"));
    }

    @Test
    void form_guestIdありならGuest() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        final VoterKeySource source = VoterKeySources.form(null, session);

        assertThat(source).isEqualTo(new VoterKeySource.Guest("abc"));
    }

    @Test
    void form_どちらも無ければMissing() {
        final VoterKeySource source = VoterKeySources.form(null, new MockHttpSession());

        assertThat(source).isEqualTo(new VoterKeySource.Missing());
    }
}
