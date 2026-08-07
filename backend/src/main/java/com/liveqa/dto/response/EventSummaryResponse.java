package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.enums.EventStatus;

public record EventSummaryResponse(
    Long id,
    String title,
    String joinCode,
    EventStatus status,
    Instant createdAt
) {

}
