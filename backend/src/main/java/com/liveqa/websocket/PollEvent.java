package com.liveqa.websocket;

/**
 * ポールに起きた変化をWebSocketで配信するイベント。
 *
 * @param type 変化の種類
 * @param poll 配信するポール集計データ
 * @author mizumura
 */
public record PollEvent(
        PollEventType type,
        PollEventPayload poll
) {
}
