package com.liveqa.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class GuestSessionTest {

    @Test
    void readGuestId_文字列なら返す() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        assertThat(GuestSession.readGuestId(session)).contains("abc");
    }

    @Test
    void readGuestId_無ければempty() {
        assertThat(GuestSession.readGuestId(new MockHttpSession())).isEmpty();
    }

    @Test
    void writeGuestId_属性に書く() {
        final MockHttpSession session = new MockHttpSession();
        GuestSession.writeGuestId(session, "xyz");

        assertThat(session.getAttribute(GuestSession.ATTR_GUEST_ID)).isEqualTo("xyz");
    }
}
