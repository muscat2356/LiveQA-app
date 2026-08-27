package com.liveqa.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.liveqa.dto.response.QuestionResponse;

/**
 * 質問イベントをWebSocket経由で配信するクラス。
 * 
 * @author mizumura
 */
@Component
public class QuestionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 質問イベントの配信に使用するオブジェクトを受け取る。
     * 
     * @param messagingTemplate STOMPメッセージを送信するSpring提供オブジェクト
     */
    public QuestionEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 質問の変化を対象イベントのSTOMP宛先へ配信する。
     * 
     * @param type 質問イベントの種類
     * @param response 配信元となる質問応答データ
     */
    public void publish(QuestionEventType type, QuestionResponse response) {
        QuestionEvent event = new QuestionEvent(
                type,
                QuestionEventPayload.from(response)
        );

        String destination = "/topic/events/" + response.eventId() + "/questions";

        messagingTemplate.convertAndSend(destination, event);
    }
}
