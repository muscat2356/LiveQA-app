package com.liveqa.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.liveqa.dto.response.PollOptionResponse;
import com.liveqa.dto.response.PollResponse;
import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;

/**
 * {@link PollEventPublisher}の単体テスト。
 *
 * @author mizumura
 */
@ExtendWith(MockitoExtension.class)
class PollEventPublisherTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    PollEventPublisher publisher;

    @Test
    void publish_正しい宛先とポールイベントを送信する() {
        final PollResponse response = new PollResponse(
                5L,
                10L,
                "次のテーマは？",
                PollType.SINGLE,
                PollStatus.OPEN,
                List.of(
                        new PollOptionResponse(31L, "AI", 2),
                        new PollOptionResponse(32L, "Web", 1)),
                3,
                List.of(31L));

        publisher.publish(PollEventType.POLL_UPDATED, response);

        final ArgumentCaptor<PollEvent> eventCaptor =
                ArgumentCaptor.forClass(PollEvent.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/events/10/polls"),
                eventCaptor.capture());

        final PollEvent actualEvent = eventCaptor.getValue();
        assertThat(actualEvent.type()).isEqualTo(PollEventType.POLL_UPDATED);
        assertThat(actualEvent.poll()).isEqualTo(PollEventPayload.from(response));
    }
}
