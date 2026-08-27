package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.domain.Event;
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

    /**
     * Entityから応答DTOへ変換するファクトリ
     * @param event イベント
     * @return 応答DTO
     */
    public static EventDetailResponse form(Event event) {
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getJoinCode(),
                event.getStatus(),
                event.getQuestionCount(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
