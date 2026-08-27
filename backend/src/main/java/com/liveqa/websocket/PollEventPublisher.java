package com.liveqa.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.liveqa.dto.response.PollResponse;

/**
 * ポールイベントをWebSocket経由で配信するクラス。
 *
 * @author mizumura
 */
@Component
public class PollEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * ポールイベントの配信に使用するオブジェクトを受け取る。
     *
     * @param messagingTemplate STOMPメッセージを送信するSpring提供オブジェクト
     */
    public PollEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * ポールの変化を対象イベントのSTOMP宛先へ配信する。
     *
     * @param type ポールイベントの種類
     * @param response 配信元となるポール応答データ
     */
    public void publish(PollEventType type, PollResponse response) {
        final PollEvent event = new PollEvent(
                type,
                PollEventPayload.from(response));
        final String destination =
                "/topic/events/" + response.eventId() + "/polls";

        messagingTemplate.convertAndSend(destination, event);
    }
}
