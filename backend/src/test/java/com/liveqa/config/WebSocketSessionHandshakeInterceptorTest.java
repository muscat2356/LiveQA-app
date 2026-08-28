package com.liveqa.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.socket.WebSocketHandler;

import com.liveqa.controller.GuestSession;

class WebSocketSessionHandshakeInterceptorTest {

    private final WebSocketSessionHandshakeInterceptor interceptor =
            new WebSocketSessionHandshakeInterceptor();

    @Test
    void beforeHandshake_guestId付きセッションなら許可する() {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "guest-1");

        assertThat(beforeHandshake(session)).isTrue();
    }

    @Test
    void beforeHandshake_認証済みセッションなら許可する() {
        final MockHttpSession session = new MockHttpSession();
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new TestingAuthenticationToken("host", null, "ROLE_HOST"));
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);

        assertThat(beforeHandshake(session)).isTrue();
    }

    @Test
    void beforeHandshake_利用者情報がないセッションなら拒否する() {
        final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setSession(new MockHttpSession());
        final ServerHttpResponse response = mock(ServerHttpResponse.class);

        final boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                mock(WebSocketHandler.class),
                new HashMap<>());

        assertThat(allowed).isFalse();
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    void beforeHandshake_HTTPセッションがなければ拒否する() {
        final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        final ServerHttpResponse response = mock(ServerHttpResponse.class);

        final boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                mock(WebSocketHandler.class),
                new HashMap<>());

        assertThat(allowed).isFalse();
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    private boolean beforeHandshake(MockHttpSession session) {
        final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setSession(session);
        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                new HashMap<>());
    }
}
