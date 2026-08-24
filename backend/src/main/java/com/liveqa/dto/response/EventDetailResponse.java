package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.enums.EventStatus;

/**
 * イベント詳細GETの応答DTO
 * EventDetailResponse
 * @author ichikura
 */
public record EventDetailResponse(
        Long id,
        String title,
        String joinCode,
        EventStatus status,
        int questionCount,
        Instant createdAt,
        Instant updatedAt
    ) {

}
