package com.liveqa.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class StompInboundAuthorizationInterceptorTest {

    private final StompInboundAuthorizationInterceptor interceptor =
            new StompInboundAuthorizationInterceptor();
    private final MessageChannel channel = mock(MessageChannel.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "/topic/events/10/questions",
            "/topic/events/10/polls"
    })
    void preSend_正規の宛先なら購読を許可する(String destination) {
        final Message<byte[]> message = message(StompCommand.SUBSCRIBE, destination);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/topic/events/0/questions",
            "/topic/events/abc/questions",
            "/topic/events/10/questions/extra",
            "/topic/other",
            "/app/events/10/questions"
    })
    void preSend_不正な宛先なら購読を拒否する(String destination) {
        final Message<byte[]> message = message(StompCommand.SUBSCRIBE, destination);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/topic/events/10/questions",
            "/topic/events/10/polls"
    })
    void preSend_クライアントからのSENDを拒否する(String destination) {
        final Message<byte[]> message = message(StompCommand.SEND, destination);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<byte[]> message(StompCommand command, String destination) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
