package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.enums.QuestionStatus;

/**
 * 質問投稿・一覧の応答DTO（同じ形）
 * votedByMeは今のセッション視点。キー未確立ならfalse
 * QuestionResponse
 * @author ichikura
 */
public record QuestionResponse(
    Long id,
    Long eventId,
    String body,
    String displayName,
    int upvoteCount,
    QuestionStatus status,
    boolean votedByMe,
    Instant createdAt
) {

}
