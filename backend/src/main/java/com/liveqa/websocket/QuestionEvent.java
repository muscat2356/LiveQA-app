package com.liveqa.websocket;

/**
 * 質問に起きた変化をWebSocketで配信するイベント。
 * 
 * @param type 変化の種類
 * @param question 配信する質問データ
 * @author mizumura
 */
public record QuestionEvent(
    QuestionEventType type,
    QuestionEventPayload question
) {

}
