package com.liveqa.dto.response;

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

}
