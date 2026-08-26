package com.liveqa.websocket;

import java.time.Instant;

import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.enums.QuestionStatus;

/**
 * WebSocketで配信する質問データ。
 * 
 * @param id          質問ID
 * @param eventId     イベントID
 * @param body        質問本文
 * @param displayName 表示名
 * @param upvoteCount いいね数
 * @param status      質問の状態
 * @param createdAt   作成日時
 * @author mizumura
 */
public record QuestionEventPayload(
        Long id,
        Long eventId,
        String body,
        String displayName,
        int upvoteCount,
        QuestionStatus status,
        Instant createdAt) {

    /**
     * REST APIの質問応答データからWebSocket配信用データを生成する。
     * @param response 変換元の質問応答データ
     * @return WebSocket配信用の質問データ
     */
    public static QuestionEventPayload from(QuestionResponse response) {
        return new QuestionEventPayload(
                response.id(),
                response.eventId(),
                response.body(),
                response.displayName(),
                response.upvoteCount(),
                response.status(),
                response.createdAt());
    }

}
