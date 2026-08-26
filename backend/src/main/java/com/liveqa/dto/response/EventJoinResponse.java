package com.liveqa.dto.response;

import com.liveqa.domain.Event;
import com.liveqa.enums.EventStatus;

/**
 * 参加コード照会GETの応答DTO（公開情報のみ）
 * EventJoinResponse
 * @author ichikura
 */
public record EventJoinResponse(
    Long eventId,
    String title,
    EventStatus status
) {

    /**
     * Entityから応答DTOへ変換するファクトリ
     * 公開面は eventId / title / status だけ
     * @param event イベント
     * @return 応答DTO
     */
    public static EventJoinResponse form(Event event) {
        return new EventJoinResponse(
                event.getId(),
                event.getTitle(),
                event.getStatus());
    }
}
