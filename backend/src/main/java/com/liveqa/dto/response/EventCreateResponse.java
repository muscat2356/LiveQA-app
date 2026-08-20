package com.liveqa.dto.response;


import java.time.Instant;

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
        String joinUrl,
        EventStatus status,
        Instant createdAt) {

}
