package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.enums.EventStatus;

public record EventDetailResponse(
        Long id,
        String title,
        String joinCode,
        String joinUrl,
        EventStatus status,
        int questionCount,
        Instant createdAt,
        Instant updatedAt
    ) {

}
