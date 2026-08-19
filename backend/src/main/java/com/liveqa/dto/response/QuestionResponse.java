package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.enums.QuestionStatus;

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
