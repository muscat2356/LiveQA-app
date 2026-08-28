package com.liveqa.config;

import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * クライアントから届くSTOMPコマンドと購読先を制限する。
 *
 * @author mizumura
 */
@Component
public class StompInboundAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern ALLOWED_SUBSCRIPTION = Pattern.compile(
            "^/topic/events/[1-9][0-9]*/(questions|polls)$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.SEND) {
            throw new AccessDeniedException("クライアントからのSTOMP送信は許可されていません。");
        }

        if (accessor.getCommand() == StompCommand.SUBSCRIBE
                && !isAllowedSubscription(accessor.getDestination())) {
            throw new AccessDeniedException("このSTOMP宛先は購読できません。");
        }

        return message;
    }

    private boolean isAllowedSubscription(String destination) {
        return destination != null && ALLOWED_SUBSCRIPTION.matcher(destination).matches();
    }
}
