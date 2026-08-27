package com.liveqa.dto.response;

import com.liveqa.domain.Event;
import com.liveqa.enums.EventStatus;

/**
 * ルームステータス変更PATCHの応答DTO
 * EventStatusResponse
 * @author ichikura
 */
public record EventStatusResponse(Long id, EventStatus status) {

    /**
     * Entityから応答DTOへ変換するファクトリ
     * @param event 更新後のイベント
     * @return 応答DTO
     */
    public static EventStatusResponse form(Event event) {
        return new EventStatusResponse(event.getId(), event.getStatus());
    }
}
