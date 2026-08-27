package com.liveqa.dto.response;

import java.time.Instant;

import com.liveqa.domain.Event;
import com.liveqa.enums.EventStatus;

/**
 * イベント作成POSTの応答DTO
 * EventCreateResponse
 * @author ichikura
 */
public record EventCreateResponse(
        Long id,
        String title,
        String joinCode,
        EventStatus status,
        Instant createdAt) {

    /**
     * Entityから応答DTOへ変換するファクトリ
     * @param event 作成したイベント
     * @return 応答DTO
     */
    public static EventCreateResponse form(Event event) {
        return new EventCreateResponse(
                event.getId(),
                event.getTitle(),
                event.getJoinCode(),
                event.getStatus(),
                event.getCreatedAt());
    }
}
