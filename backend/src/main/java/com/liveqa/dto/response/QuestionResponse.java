package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.domain.Question;
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

    /**
     * Entityから応答DTOへ変換するファクトリ
     * votedByMeはEntityに無いのでServiceが渡す
     * @param question 質問
     * @param eventId イベントid
     * @param votedByMe 今のセッションがいいね済みか
     * @return 応答DTO
     */
    public static QuestionResponse form(
            Question question,
            Long eventId,
            boolean votedByMe) {
        return new QuestionResponse(
                question.getId(),
                eventId,
                question.getBody(),
                question.getDisplayName(),
                question.getUpvoteCount(),
                question.getStatus(),
                votedByMe,
                question.getCreatedAt());
    }
}
