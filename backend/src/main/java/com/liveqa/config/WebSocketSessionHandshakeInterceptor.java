package com.liveqa.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.liveqa.controller.GuestSession;

import jakarta.servlet.http.HttpSession;

/**
 * WebSocket接続時にRESTと同じHTTPセッションを検証する。
 * HOSTの認証情報または参加者のguestIdがあるセッションだけ接続を許可する。
 *
 * @author mizumura
 */
@Component
public class WebSocketSessionHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return reject(response);
        }

        final HttpSession session = servletRequest.getServletRequest().getSession(false);
        if (session == null) {
            return reject(response);
        }

        if (hasAuthenticatedUser(session) || GuestSession.readGuestId(session).isPresent()) {
            return true;
        }
        return reject(response);
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // 接続後に追加処理は不要
    }

    private boolean hasAuthenticatedUser(HttpSession session) {
        final Object attribute = session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (!(attribute instanceof SecurityContext securityContext)) {
            return false;
        }

        final Authentication authentication = securityContext.getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean reject(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }
}
