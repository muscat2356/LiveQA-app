package com.liveqa.dto.response;

import com.liveqa.enums.EventStatus;

/**
 * ルームステータス変更PATCHの応答DTO
 * EventStatusResponse
 * @author ichikura
 */
public record EventStatusResponse(Long id, EventStatus status) {

}
