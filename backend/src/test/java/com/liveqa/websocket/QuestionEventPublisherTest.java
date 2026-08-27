package com.liveqa.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.enums.QuestionStatus;

/**
 * {@link QuestionEventPublisher}の単体テスト
 * 
 * @author mizumura
 */
@ExtendWith(MockitoExtension.class)
class QuestionEventPublisherTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    QuestionEventPublisher publisher;

    @Test
    void publish_正しい宛先と質問イベントを送信する() {
        QuestionResponse response = new QuestionResponse(
                42L,
                10L,
                "STOMPとは何ですか？",
                "Member C",
                0,
                QuestionStatus.OPEN,
                false,
                Instant.parse("2026-08-26T00:00:00Z"));

        publisher.publish(QuestionEventType.QUESTION_CREATED, response);

        ArgumentCaptor<QuestionEvent> eventCaptor = ArgumentCaptor.forClass(QuestionEvent.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/events/10/questions"),
                eventCaptor.capture());

        QuestionEvent actualEvent = eventCaptor.getValue();

        assertThat(actualEvent.type())
                .isEqualTo(QuestionEventType.QUESTION_CREATED);

        assertThat(actualEvent.question())
                .isEqualTo(QuestionEventPayload.from(response));
    }
}
