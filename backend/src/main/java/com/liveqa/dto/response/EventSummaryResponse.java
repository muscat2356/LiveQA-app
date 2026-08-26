package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.domain.Event;
import com.liveqa.enums.EventStatus;

/**
 * 自分のイベント一覧GETの応答DTO（1件分）
 * EventSummaryResponse
 * @author ichikura
 */
public record EventSummaryResponse(
    Long id,
    String title,
    String joinCode,
    EventStatus status,
    Instant createdAt
) {

    /**
     * Entityから応答DTOへ変換するファクトリ
     * @param event イベント
     * @return 応答DTO
     */
    public static EventSummaryResponse form(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getJoinCode(),
                event.getStatus(),
                event.getCreatedAt());
    }
}
