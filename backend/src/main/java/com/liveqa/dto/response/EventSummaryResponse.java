package com.liveqa.dto.response;

import java.time.Instant;

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

}
